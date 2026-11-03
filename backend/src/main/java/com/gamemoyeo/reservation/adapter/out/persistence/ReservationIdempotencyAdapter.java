package com.gamemoyeo.reservation.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.ReservationUseCase.ParticipationView;
import com.gamemoyeo.reservation.application.port.out.ReservationIdempotencyPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationIdempotencyAdapter implements ReservationIdempotencyPort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ReservationIdempotencyAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }
    @Override
    public Optional<ParticipationView> replay(long memberId, String operation, String key, String payload) {
        // The caller holds the meetup row lock. Operation contains its ID, so all writers for this key serialize.
        return jdbc.query("SELECT request_hash, response_body FROM idempotency_record WHERE member_id=? AND operation=? AND BINARY idempotency_key=BINARY ?",
            (row, index) -> {
                if (!hash(payload).equals(row.getString(1))) {
                    throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_PAYLOAD_MISMATCH", "This request key was used with different input.");
                }
                try { return json.readValue(row.getString(2), ParticipationView.class); }
                catch (JsonProcessingException error) { throw new IllegalStateException("Invalid stored response", error); }
            }, memberId, operation, key).stream().findFirst();
    }
    @Override
    public void save(long memberId, String operation, String key, String payload, ParticipationView result) {
        try {
            jdbc.update("""
                INSERT INTO idempotency_record(member_id,operation,idempotency_key,request_hash,response_status,response_body,expires_at)
                VALUES (?,?,?,?,200,?,DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 30 DAY))
                """, memberId, operation, key, hash(payload), json.writeValueAsString(result));
        } catch (JsonProcessingException error) { throw new IllegalStateException("Cannot encode response", error); }
    }
    private String hash(String payload) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
