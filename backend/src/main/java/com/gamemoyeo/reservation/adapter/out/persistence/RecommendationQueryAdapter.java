package com.gamemoyeo.reservation.adapter.out.persistence;

import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.reservation.application.RecommendationUseCase.Item;
import com.gamemoyeo.reservation.application.RecommendationUseCase.Query;
import com.gamemoyeo.reservation.application.port.out.RecommendationPort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class RecommendationQueryAdapter implements RecommendationPort {
    private final DSLContext dsl;
    public RecommendationQueryAdapter(DSLContext dsl) { this.dsl = dsl; }
    private static final String VACANCY = """
        EXISTS (SELECT 1 FROM meetup_role_requirement rr JOIN game_option role ON role.id=rr.role_option_id
        WHERE rr.meetup_id=m.id AND (CASE WHEN role.code='BOTTOM' THEN 'ADC' ELSE role.code END)=?
        AND rr.capacity>(SELECT COUNT(*) FROM reservation pr WHERE pr.session_id=s.id
        AND pr.status='CONFIRMED' AND pr.primary_position=?))
        """;
    @Override
    public List<Item> find(long memberId, Query query, PositionPreferences positions, Integer tier, Instant now,
        Integer cursorScore, Instant cursorTime, Long cursorId, int limit) {
        var args = new ArrayList<Object>();
        String primary = positions.primaryPosition()==null?null:positions.primaryPosition().name();
        String secondary = positions.secondaryPosition()==null?null:positions.secondaryPosition().name();
        args.add(primary);
        args.add(primary);
        args.add(secondary);
        args.add(secondary);
        var sql = new StringBuilder("SELECT * FROM (SELECT m.id,m.title,s.starts_at,s.capacity-s.reserved_count AS remaining,CASE WHEN ")
            .append(VACANCY).append(" THEN 100 WHEN ").append(VACANCY).append(" THEN 60 ELSE 0 END AS score ")
            .append("""
                FROM meetup m JOIN meetup_session s ON s.meetup_id=m.id
                JOIN game g ON g.id=m.game_id
                LEFT JOIN game_option lo ON lo.id=m.minimum_tier_option_id
                LEFT JOIN game_option hi ON hi.id=m.maximum_tier_option_id
                WHERE m.game_id=? AND g.status='ACTIVE' AND m.status='OPEN' AND s.status='OPEN'
                AND s.reserved_count<s.capacity AND s.starts_at>?
                AND (m.recruitment_deadline IS NULL OR m.recruitment_deadline>?)
                AND m.owner_id<>? AND NOT EXISTS (SELECT 1 FROM reservation joined
                WHERE joined.session_id=s.id AND joined.member_id=? AND joined.status='CONFIRMED')
                AND NOT EXISTS (SELECT 1 FROM member_block b JOIN reservation blocked ON
                  ((b.member_id=? AND b.blocked_member_id=blocked.member_id) OR (b.blocked_member_id=? AND b.member_id=blocked.member_id))
                  WHERE blocked.session_id=s.id AND blocked.status='CONFIRMED')
                """);
        args.add(query.gameId());
        args.add(utc(now.plusSeconds(600)));
        args.add(utc(now));
        args.add(memberId);
        args.add(memberId);
        args.add(memberId);
        args.add(memberId);
        if (query.regionId()!=null) { sql.append(" AND m.region_option_id=?");
            args.add(query.regionId()); }
        if (query.modeId()!=null) { sql.append(" AND m.mode_option_id=?");
            args.add(query.modeId()); }
        if (query.playStyle()!=null) { sql.append(" AND m.play_style=?");
            args.add(query.playStyle()); }
        if (query.from()!=null) { sql.append(" AND s.starts_at>=?");
            args.add(utc(query.from())); }
        if (query.to()!=null) { sql.append(" AND s.starts_at<?");
            args.add(utc(query.to())); }
        if (tier!=null) {
            sql.append(" AND (lo.id IS NULL OR lo.sort_order<=?) AND (hi.id IS NULL OR hi.sort_order>=?)");
            args.add(tier);
            args.add(tier);
        }
        sql.append(") ranked");
        if (cursorScore!=null) {
            sql.append(" WHERE score<? OR (score=? AND (starts_at>? OR (starts_at=? AND id>?)))");
            args.add(cursorScore);
            args.add(cursorScore);
            args.add(utc(cursorTime));
            args.add(utc(cursorTime));
            args.add(cursorId);
        }
        sql.append(" ORDER BY score DESC,starts_at,id LIMIT ?");
        args.add(limit);
        return dsl.fetch(sql.toString(),args.toArray()).map(row -> {
            int score = row.get("score",Integer.class);
            return new Item(row.get("id",Long.class),row.get("title",String.class),
                row.get("starts_at",LocalDateTime.class).toInstant(ZoneOffset.UTC),row.get("remaining",Integer.class),
                score==100?"PRIMARY_POSITION":score==60?"SECONDARY_POSITION":"FILTER_MATCH",score);
        });
    }
    private LocalDateTime utc(Instant time) { return LocalDateTime.ofInstant(time,ZoneOffset.UTC); }
}
