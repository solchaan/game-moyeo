package com.gamemoyeo.reservation.adapter.out.persistence;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.notification.application.NotificationUseCase;
import com.gamemoyeo.reservation.application.WaitlistUseCase.View;
import com.gamemoyeo.reservation.application.port.out.WaitlistPort;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WaitlistJdbcAdapter implements WaitlistPort {
    private final JdbcTemplate jdbc;
    private final NotificationUseCase notifications;
    public WaitlistJdbcAdapter(JdbcTemplate jdbc, NotificationUseCase notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }
    @Override
    public View find(long meetup, long member, boolean canJoin) {
        return jdbc.query("""
            SELECT w.status,w.offered_until,
            (SELECT COUNT(*) FROM reservation_waitlist ahead WHERE ahead.meetup_id=w.meetup_id
             AND ahead.status IN ('WAITING','OFFERED') AND (ahead.joined_at<w.joined_at OR (ahead.joined_at=w.joined_at AND ahead.id<=w.id))) AS position,
            w.status='OFFERED' AND w.offered_until>UTC_TIMESTAMP(6) AS can_accept
            FROM reservation_waitlist w WHERE w.meetup_id=? AND w.member_id=?
            """, (row,index) -> {
                String status = row.getString(1);
                boolean active = List.of("WAITING","OFFERED").contains(status);
                var until = row.getObject(2,LocalDateTime.class);
                return new View(status,active?row.getInt(3):null,until==null?null:until.toInstant(ZoneOffset.UTC),
                    canJoin&&!active,row.getBoolean(4),active);
            },meetup,member).stream().findFirst().orElse(new View("NONE",null,null,canJoin,false,false));
    }
    @Override
    public void join(long meetup, long member) {
        jdbc.update("""
            INSERT INTO reservation_waitlist(meetup_id,member_id) VALUES (?,?)
            ON DUPLICATE KEY UPDATE status='WAITING',joined_at=UTC_TIMESTAMP(6),offered_until=NULL
            """,meetup,member);
    }
    @Override
    public void cancel(long meetup, long member) {
        jdbc.update("UPDATE reservation_waitlist SET status='CANCELLED',offered_until=NULL WHERE meetup_id=? AND member_id=? AND status IN ('WAITING','OFFERED')",meetup,member);
    }
    @Override
    public List<Long> pendingMeetups() {
        return jdbc.queryForList("""
            SELECT DISTINCT w.meetup_id FROM reservation_waitlist w
            JOIN meetup_session s ON s.meetup_id=w.meetup_id JOIN meetup m ON m.id=w.meetup_id
            WHERE w.status IN ('WAITING','OFFERED') AND (
              (w.status='OFFERED' AND w.offered_until<=UTC_TIMESTAMP(6))
              OR (s.capacity>s.reserved_count AND NOT EXISTS (
                SELECT 1 FROM reservation_waitlist offered WHERE offered.meetup_id=w.meetup_id
                AND offered.status='OFFERED' AND offered.offered_until>UTC_TIMESTAMP(6)))
              OR m.status='DELETED' OR (s.closed_reason IS NOT NULL AND s.closed_reason<>'FULL')
              OR s.starts_at<=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE)
              OR m.recruitment_deadline<=UTC_TIMESTAMP(6))
            ORDER BY w.meetup_id LIMIT 100
            """,Long.class);
    }
    @Override
    public void advance(long meetup) {
        jdbc.update("UPDATE reservation_waitlist SET status='EXPIRED' WHERE meetup_id=? AND status='OFFERED' AND offered_until<=UTC_TIMESTAMP(6)",meetup);
        boolean eligible = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT m.status<>'DELETED' AND (s.closed_reason IS NULL OR s.closed_reason='FULL')
            AND s.starts_at>DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE)
            AND (m.recruitment_deadline IS NULL OR m.recruitment_deadline>UTC_TIMESTAMP(6))
            FROM meetup m JOIN meetup_session s ON s.meetup_id=m.id WHERE m.id=?
            """,Boolean.class,meetup));
        if (!eligible) {
            jdbc.update("UPDATE reservation_waitlist SET status='EXPIRED' WHERE meetup_id=? AND status IN ('WAITING','OFFERED')",meetup);
            return;
        }
        var offered = jdbc.queryForList("SELECT member_id FROM reservation_waitlist WHERE meetup_id=? AND status='OFFERED'",Long.class,meetup);
        if (!offered.isEmpty()) { return; }
        Integer remaining = jdbc.queryForObject("SELECT capacity-reserved_count FROM meetup_session WHERE meetup_id=?",Integer.class,meetup);
        if (remaining==null || remaining<=0) { return; }
        var next = jdbc.queryForList("SELECT member_id FROM reservation_waitlist WHERE meetup_id=? AND status='WAITING' ORDER BY joined_at,id LIMIT 1",Long.class,meetup);
        if (next.isEmpty()) { return; }
        jdbc.update("""
            UPDATE reservation_waitlist w JOIN meetup_session s ON s.meetup_id=w.meetup_id
            JOIN meetup m ON m.id=w.meetup_id
            SET w.status='OFFERED',w.offered_until=LEAST(DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 5 MINUTE),
                DATE_SUB(s.starts_at,INTERVAL 10 MINUTE),COALESCE(m.recruitment_deadline,s.starts_at))
            WHERE w.meetup_id=? AND w.member_id=?
            """,meetup,next.getFirst());
        notifications.publish(next,meetup,"WAITLIST_OFFER","빈자리가 생겼습니다. 모임에서 표시된 기한 안에 참가를 확정해 주세요.",null);
    }
    @Override
    public void guardAdmission(long meetup, long member) {
        advance(meetup);
        var offered = jdbc.queryForList("SELECT member_id FROM reservation_waitlist WHERE meetup_id=? AND status='OFFERED' AND offered_until>UTC_TIMESTAMP(6)",Long.class,meetup);
        if (!offered.isEmpty() && offered.getFirst()!=member) {
            throw new ApiException(HttpStatus.CONFLICT,"WAITLIST_PRIORITY","A waiting participant has priority for this vacancy.");
        }
    }
    @Override
    public void accepted(long meetup, long member) {
        jdbc.update("UPDATE reservation_waitlist SET status='ACCEPTED' WHERE meetup_id=? AND member_id=? AND status IN ('WAITING','OFFERED')",meetup,member);
        advance(meetup);
    }
}
