package com.gamemoyeo.reservation.adapter.out.persistence;

import com.gamemoyeo.reservation.application.port.out.ReservationPort;
import com.gamemoyeo.reservation.application.port.out.ReservationPort.ReservationState;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationJdbcAdapter implements ReservationPort {

    private final JdbcTemplate jdbc;

    public ReservationJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean initializeOwner(long meetupId, long ownerId) {
        return jdbc.update("""
            INSERT INTO reservation (session_id, member_id, party_size, status)
            SELECT session.id, meetup.owner_id, 1, 'CONFIRMED'
            FROM meetup_session session
            JOIN meetup ON meetup.id = session.meetup_id
            WHERE meetup.id = ? AND meetup.owner_id = ?
            """, meetupId, ownerId) == 1;
    }

    @Override
    public Optional<ReservationState> findState(long meetupId, Long memberId) {
        String sql = """
            SELECT meetup.id AS meetup_id,
                   session.id AS session_id,
                   meetup.owner_id,
                   meetup.status AS meetup_status,
                   session.status AS session_status,
                   session.closed_reason,
                   session.starts_at,
                   meetup.recruitment_deadline,
                   session.capacity,
                   session.reserved_count,
                   reservation.status AS reservation_status
            FROM meetup
            JOIN meetup_session session ON session.meetup_id = meetup.id
            LEFT JOIN reservation
              ON reservation.session_id = session.id
             AND reservation.member_id = ?
            WHERE meetup.id = ?
            """;
        return jdbc.query(sql, this::mapState, memberId == null ? -1L : memberId, meetupId)
            .stream().findFirst();
    }

    @Override
    public boolean lockMeetup(long meetupId) {
        // Serialize writers before reading state or locking reservation index gaps.
        // All participation mutations take the parent lock before child rows.
        return !jdbc.queryForList("SELECT id FROM meetup WHERE id = ? FOR UPDATE",
            Long.class, meetupId).isEmpty();
    }

    @Override
    public void confirmReservation(long sessionId, long memberId) {
        int restored = jdbc.update("""
            UPDATE reservation
            SET status = 'CONFIRMED', cancelled_at = NULL
            WHERE session_id = ? AND member_id = ? AND status = 'CANCELLED'
            """, sessionId, memberId);
        if (restored == 0) {
            jdbc.update("""
                INSERT INTO reservation (session_id, member_id, party_size, status)
                VALUES (?, ?, 1, 'CONFIRMED')
                """, sessionId, memberId);
        }
    }

    @Override
    public boolean claimSeat(long meetupId) {
        return jdbc.update("""
            UPDATE meetup_session session
            JOIN meetup ON meetup.id = session.meetup_id
            SET session.reserved_count = session.reserved_count + 1,
                session.version = session.version + 1
            WHERE meetup.id = ?
              AND meetup.status = 'OPEN'
              AND session.status = 'OPEN'
              AND session.reserved_count < session.capacity
              AND session.starts_at > DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
              AND (meetup.recruitment_deadline IS NULL
                   OR meetup.recruitment_deadline > UTC_TIMESTAMP(6))
            """, meetupId) == 1;
    }

    @Override
    public void closeIfFull(long meetupId) {
        jdbc.update("""
            UPDATE meetup_session session
            JOIN meetup ON meetup.id = session.meetup_id
            SET session.status = 'CLOSED',
                session.closed_reason = 'FULL',
                session.version = session.version + 1,
                meetup.status = 'CLOSED',
                meetup.version = meetup.version + 1
            WHERE meetup.id = ?
              AND session.status = 'OPEN'
              AND session.reserved_count >= session.capacity
            """, meetupId);
    }

    @Override
    public boolean cancelReservation(long sessionId, long memberId) {
        return jdbc.update("""
            UPDATE reservation
            SET status = 'CANCELLED', cancelled_at = UTC_TIMESTAMP(6)
            WHERE session_id = ? AND member_id = ? AND status = 'CONFIRMED'
            """, sessionId, memberId) == 1;
    }

    @Override
    public void releaseSeat(long meetupId) {
        jdbc.update("""
            UPDATE meetup_session session
            SET session.reserved_count = session.reserved_count - 1,
                session.version = session.version + 1
            WHERE session.meetup_id = ? AND session.reserved_count > 1
            """, meetupId);
    }

    @Override
    public void reopenIfPossible(long meetupId) {
        jdbc.update("""
            UPDATE meetup_session session
            JOIN meetup ON meetup.id = session.meetup_id
            SET session.status = 'OPEN',
                session.closed_reason = NULL,
                session.version = session.version + 1,
                meetup.status = 'OPEN',
                meetup.version = meetup.version + 1
            WHERE meetup.id = ?
              AND session.closed_reason = 'FULL'
              AND session.reserved_count < session.capacity
              AND session.starts_at > DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
              AND (meetup.recruitment_deadline IS NULL
                   OR meetup.recruitment_deadline > UTC_TIMESTAMP(6))
            """, meetupId);
    }

    @Override
    public boolean closeByOwner(long meetupId, long ownerId) {
        return jdbc.update("""
            UPDATE meetup_session session
            JOIN meetup ON meetup.id = session.meetup_id
            SET session.status = 'CLOSED',
                session.closed_reason = 'OWNER_CLOSED',
                session.version = session.version + 1,
                meetup.status = 'CLOSED',
                meetup.version = meetup.version + 1
            WHERE meetup.id = ?
              AND meetup.owner_id = ?
              AND meetup.status = 'OPEN'
              AND session.status = 'OPEN'
            """, meetupId, ownerId) == 1;
    }

    @Override
    public void closeDueMeetups() {
        jdbc.update("""
            UPDATE meetup_session session
            JOIN meetup ON meetup.id = session.meetup_id
            SET session.status = 'CLOSED',
                session.closed_reason = CASE
                    WHEN session.starts_at <= DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
                    THEN 'START_IMMINENT'
                    ELSE 'DEADLINE_REACHED'
                END,
                session.version = session.version + 1,
                meetup.status = 'CLOSED',
                meetup.version = meetup.version + 1
            WHERE meetup.status = 'OPEN'
              AND session.status = 'OPEN'
              AND (
                  session.starts_at <= DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
                  OR (meetup.recruitment_deadline IS NOT NULL
                      AND meetup.recruitment_deadline <= UTC_TIMESTAMP(6))
              )
            """);
    }

    private ReservationState mapState(ResultSet result, int rowNumber) throws SQLException {
        return new ReservationState(
            result.getLong("meetup_id"),
            result.getLong("session_id"),
            result.getLong("owner_id"),
            result.getString("meetup_status"),
            result.getString("session_status"),
            result.getString("closed_reason"),
            result.getTimestamp("starts_at").toInstant(),
            result.getTimestamp("recruitment_deadline") == null
                ? null : result.getTimestamp("recruitment_deadline").toInstant(),
            result.getInt("capacity"),
            result.getInt("reserved_count"),
            result.getString("reservation_status")
        );
    }
}
