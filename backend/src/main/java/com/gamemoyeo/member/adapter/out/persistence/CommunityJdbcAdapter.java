package com.gamemoyeo.member.adapter.out.persistence;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.CommunityUseCase.Companion;
import com.gamemoyeo.member.application.CommunityUseCase.Blocked;
import com.gamemoyeo.member.application.CommunityUseCase.Report;
import com.gamemoyeo.member.application.port.out.CommunityPort;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CommunityJdbcAdapter implements CommunityPort {
    private final JdbcTemplate jdbc;
    public CommunityJdbcAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public List<Companion> companions(long member, Long cursor, int limit) {
        return jdbc.query("""
            SELECT other.id,m.id,m.title,p.id,p.nickname,b.member_id IS NOT NULL,e.member_id IS NOT NULL
            FROM reservation mine JOIN meetup_session s ON s.id=mine.session_id
            JOIN meetup m ON m.id=s.meetup_id JOIN reservation other ON other.session_id=s.id AND other.member_id<>mine.member_id
            JOIN member p ON p.id=other.member_id
            LEFT JOIN member_block b ON b.member_id=mine.member_id AND b.blocked_member_id=p.id
            LEFT JOIN member_endorsement e ON e.member_id=mine.member_id AND e.target_member_id=p.id AND e.meetup_id=m.id
            WHERE mine.member_id=? AND mine.status='CONFIRMED' AND other.status='CONFIRMED'
            AND s.starts_at<UTC_TIMESTAMP(6) AND m.status<>'DELETED' AND other.id<?
            ORDER BY other.id DESC LIMIT ?
            """,(row,index)->new Companion(row.getLong(1),row.getLong(2),row.getString(3),row.getLong(4),row.getString(5),row.getBoolean(6),row.getBoolean(7)),
            member,cursor==null?Long.MAX_VALUE:cursor,limit);
    }
    @Override
    public List<Blocked> blocks(long member, Long cursor, int limit) {
        return jdbc.query("SELECT p.id,p.nickname FROM member_block b JOIN member p ON p.id=b.blocked_member_id WHERE b.member_id=? AND p.id<? ORDER BY p.id DESC LIMIT ?",
            (row,index)->new Blocked(row.getLong(1),row.getString(2)),member,cursor==null?Long.MAX_VALUE:cursor,limit);
    }
    @Override
    public void block(long member, long target, boolean blocked) {
        var ids = jdbc.queryForList("SELECT id FROM member WHERE id IN (?,?) ORDER BY id FOR UPDATE",Long.class,member,target);
        if (ids.size()!=2) { throw new ApiException(HttpStatus.NOT_FOUND,"MEMBER_NOT_FOUND","Member was not found."); }
        if (blocked) {
            jdbc.update("INSERT INTO member_block(member_id,blocked_member_id) VALUES (?,?) ON DUPLICATE KEY UPDATE member_id=member_id",member,target);
        } else { jdbc.update("DELETE FROM member_block WHERE member_id=? AND blocked_member_id=?",member,target); }
    }
    @Override
    public boolean sharedPast(long member, long target, long meetup) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM meetup_session s JOIN meetup m ON m.id=s.meetup_id
            JOIN reservation mine ON mine.session_id=s.id AND mine.member_id=? AND mine.status='CONFIRMED'
            JOIN reservation other ON other.session_id=s.id AND other.member_id=? AND other.status='CONFIRMED'
            WHERE s.meetup_id=? AND s.starts_at<UTC_TIMESTAMP(6) AND m.status<>'DELETED')
            """,Boolean.class,member,target,meetup));
    }
    @Override
    public void endorse(long member, long target, long meetup, boolean endorsed) {
        if (endorsed) {
            jdbc.update("INSERT INTO member_endorsement(member_id,target_member_id,meetup_id) VALUES (?,?,?) ON DUPLICATE KEY UPDATE member_id=member_id",member,target,meetup);
        } else { jdbc.update("DELETE FROM member_endorsement WHERE member_id=? AND target_member_id=? AND meetup_id=?",member,target,meetup); }
    }
    @Override
    public void report(long member, long target, long meetup, String reason, String details) {
        jdbc.update("""
            INSERT INTO member_report(reporter_id,target_member_id,meetup_id,reason,details) VALUES (?,?,?,?,?)
            ON DUPLICATE KEY UPDATE id=id
            """,member,target,meetup,reason,details);
    }
    @Override
    public List<Report> reports(Long cursor, int limit) {
        return jdbc.query("SELECT id,reporter_id,target_member_id,meetup_id,reason,details,status,resolution FROM member_report WHERE id<? ORDER BY id DESC LIMIT ?",
            (row,index)->new Report(row.getLong(1),row.getLong(2),row.getLong(3),row.getLong(4),row.getString(5),row.getString(6),row.getString(7),row.getString(8)),
            cursor==null?Long.MAX_VALUE:cursor,limit);
    }
    @Override
    public void resolve(long admin, long report, String status, String resolution) {
        int updated = jdbc.update("UPDATE member_report SET status=?,resolution=?,resolved_by=?,resolved_at=UTC_TIMESTAMP(6) WHERE id=? AND status='OPEN'",status,resolution,admin,report);
        if (updated==0) { throw new ApiException(HttpStatus.CONFLICT,"REPORT_ALREADY_CLOSED","The report is missing or has already been processed."); }
    }
    @Override
    public void guardAdmission(long meetup, long member) {
        // Block and admission serialize on ordered member locks; the reservation caller already owns the meetup lock.
        var participants = jdbc.queryForList("SELECT r.member_id FROM reservation r JOIN meetup_session s ON s.id=r.session_id WHERE s.meetup_id=? AND r.status='CONFIRMED'",Long.class,meetup);
        var ids = new java.util.ArrayList<>(participants);
        ids.add(member);
        String marks = String.join(",",java.util.Collections.nCopies(ids.size(),"?"));
        jdbc.queryForList("SELECT id FROM member WHERE id IN ("+marks+") ORDER BY id FOR UPDATE",Long.class,ids.toArray());
        Boolean conflict = jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM member_block b JOIN reservation r ON
            ((b.member_id=? AND b.blocked_member_id=r.member_id) OR (b.blocked_member_id=? AND b.member_id=r.member_id))
            JOIN meetup_session s ON s.id=r.session_id WHERE s.meetup_id=? AND r.status='CONFIRMED')
            """,Boolean.class,member,member,meetup);
        if (Boolean.TRUE.equals(conflict)) { throw new ApiException(HttpStatus.CONFLICT,"PARTICIPATION_UNAVAILABLE","현재 이 모임에 참가할 수 없습니다. 다른 모임을 선택해 주세요."); }
    }
}
