package com.gamemoyeo.meetup.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.MeetupTemplateUseCase.Item;
import com.gamemoyeo.meetup.application.MeetupTemplateUseCase.Created;
import com.gamemoyeo.meetup.application.port.out.MeetupTemplatePort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MeetupTemplateJdbcAdapter implements MeetupTemplatePort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public MeetupTemplateJdbcAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }
    @Override
    public long save(long member, long source, MeetupCommand command) {
        jdbc.update("""
            INSERT INTO meetup_template(member_id,source_meetup_id,title,payload) VALUES (?,?,?,?)
            ON DUPLICATE KEY UPDATE title=VALUES(title),payload=VALUES(payload)
            """,member,source,command.title(),encode(command));
        return jdbc.queryForObject("SELECT id FROM meetup_template WHERE member_id=? AND source_meetup_id=?",Long.class,member,source);
    }
    @Override
    public List<Item> find(long member, Long cursor, int limit) {
        return jdbc.query("SELECT id,title FROM meetup_template WHERE member_id=? AND id<? ORDER BY id DESC LIMIT ?",
            (row,index)->new Item(row.getLong(1),row.getString(2)),member,cursor==null?Long.MAX_VALUE:cursor,limit);
    }
    @Override
    public void delete(long member, long template) { jdbc.update("DELETE FROM meetup_template WHERE id=? AND member_id=?",template,member); }
    @Override
    public MeetupCommand lock(long member, long template) {
        return jdbc.query("SELECT payload FROM meetup_template WHERE id=? AND member_id=? FOR UPDATE",
            (row,index)->decode(row.getString(1),MeetupCommand.class),template,member).stream().findFirst()
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"TEMPLATE_NOT_FOUND","Template was not found."));
    }
    @Override
    public Optional<Created> replay(long member, long template, String key, String payload) {
        return jdbc.query("SELECT request_hash,response_body FROM idempotency_record WHERE member_id=? AND operation=? AND idempotency_key=?",
            (row,index)->{
                if (!hash(payload).equals(row.getString(1))) { throw new ApiException(HttpStatus.CONFLICT,"IDEMPOTENCY_PAYLOAD_MISMATCH","This request key was used with different input."); }
                return decode(row.getString(2),Created.class);
            },member,"REPEAT:"+template,key).stream().findFirst();
    }
    @Override
    public void remember(long member, long template, String key, String payload, Created result) {
        jdbc.update("""
            INSERT INTO idempotency_record(member_id,operation,idempotency_key,request_hash,response_status,response_body,expires_at)
            VALUES (?,?,?,?,201,?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 30 DAY))
            """,member,"REPEAT:"+template,key,hash(payload),encode(result));
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException error) { throw new IllegalStateException(error); }
    }
    private <T> T decode(String value, Class<T> type) {
        try { return json.readValue(value,type); }
        catch (JsonProcessingException error) { throw new IllegalStateException(error); }
    }
    private String hash(String payload) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
