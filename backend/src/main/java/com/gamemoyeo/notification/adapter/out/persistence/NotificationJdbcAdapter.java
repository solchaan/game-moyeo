package com.gamemoyeo.notification.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamemoyeo.notification.application.NotificationUseCase.Item;
import com.gamemoyeo.notification.application.port.out.NotificationPort;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationJdbcAdapter implements NotificationPort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public NotificationJdbcAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }
    private record Payload(long memberId, long meetupId, String kind, String message) { }
    @Override
    public void enqueue(long memberId, long meetupId, String kind, String message, String deduplicationKey) {
        String id = deduplicationKey == null ? UUID.randomUUID().toString()
            : UUID.nameUUIDFromBytes((kind + ":" + meetupId + ":" + memberId + ":" + deduplicationKey).getBytes(StandardCharsets.UTF_8)).toString();
        try {
            jdbc.update("""
                INSERT INTO outbox_event(id,aggregate_type,aggregate_id,event_type,payload,occurred_at)
                VALUES (?,'meetup',?,'MEMBER_NOTIFICATION',?,UTC_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE id=id
                """, id, String.valueOf(meetupId), json.writeValueAsString(new Payload(memberId, meetupId, kind, message)));
        } catch (JsonProcessingException error) { throw new IllegalStateException(error); }
    }
    @Override
    public List<Item> find(long memberId, Long cursor, int limit) {
        return jdbc.query("""
            SELECT id,meetup_id,kind,message,created_at,read_at FROM member_notification
            WHERE member_id=? AND id<? ORDER BY id DESC LIMIT ?
            """, (row, index) -> new Item(row.getLong(1), row.getLong(2), row.getString(3), row.getString(4),
                row.getObject(5, LocalDateTime.class).toInstant(ZoneOffset.UTC), row.getTimestamp(6) != null),
            memberId, cursor == null ? Long.MAX_VALUE : cursor, limit);
    }
    @Override
    public void read(long memberId, long notificationId) {
        jdbc.update("UPDATE member_notification SET read_at=COALESCE(read_at,UTC_TIMESTAMP(6)) WHERE id=? AND member_id=?",
            notificationId, memberId);
    }
    @Override
    public void dispatch() {
        var events = jdbc.query("""
            SELECT id,payload FROM outbox_event WHERE event_type='MEMBER_NOTIFICATION'
            AND published_at IS NULL AND attempts<5 ORDER BY occurred_at,id LIMIT 100 FOR UPDATE SKIP LOCKED
            """, (row, index) -> new String[]{row.getString(1), row.getString(2)});
        for (String[] event : events) {
            Payload payload;
            try { payload = json.readValue(event[1], Payload.class); }
            catch (JsonProcessingException error) {
                jdbc.update("UPDATE outbox_event SET attempts=attempts+1 WHERE id=?", event[0]);
                continue;
            }
            try {
                jdbc.update("""
                INSERT INTO member_notification(event_id,member_id,meetup_id,kind,message)
                VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE event_id=event_id
                """, event[0], payload.memberId(), payload.meetupId(), payload.kind(), payload.message());
            } catch (org.springframework.dao.DataIntegrityViolationException error) {
                jdbc.update("UPDATE outbox_event SET attempts=attempts+1 WHERE id=?", event[0]);
                continue;
            }
            jdbc.update("UPDATE outbox_event SET published_at=UTC_TIMESTAMP(6),attempts=attempts+1 WHERE id=?", event[0]);
        }
    }
}
