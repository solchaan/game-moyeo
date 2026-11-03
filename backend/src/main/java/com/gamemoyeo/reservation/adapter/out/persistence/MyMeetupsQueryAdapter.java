package com.gamemoyeo.reservation.adapter.out.persistence;

import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Filter;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Item;
import com.gamemoyeo.reservation.application.port.out.MyMeetupsPort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public class MyMeetupsQueryAdapter implements MyMeetupsPort {
    private final DSLContext dsl;
    public MyMeetupsQueryAdapter(DSLContext dsl) { this.dsl = dsl; }

    @Override
    public List<Item> find(long memberId, Filter filter, Instant now, Instant cursorTime, Long cursorId, int limit) {
        var id = field("m", "id", Long.class);
        var owner = field("m", "owner_id", Long.class);
        var game = field("m", "game_id", Long.class);
        var title = field("m", "title", String.class);
        var status = field("m", "status", String.class);
        var start = field("s", "starts_at", LocalDateTime.class);
        var count = field("s", "reserved_count", Integer.class);
        var capacity = field("s", "capacity", Integer.class);
        var ready = field("r", "ready", Boolean.class);
        var reservationStatus = field("r", "status", String.class);
        var condition = filter == Filter.HOSTED ? owner.eq(memberId)
            : owner.eq(memberId).or(reservationStatus.eq("CONFIRMED"));
        var utc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        if (filter == Filter.UPCOMING) { condition = condition.and(start.ge(utc)).and(status.ne("DELETED")); }
        if (filter == Filter.PAST) { condition = condition.and(start.lt(utc)); }
        boolean ascending = filter == Filter.UPCOMING;
        if (cursorTime != null) {
            var cursor = LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC);
            condition = condition.and(ascending
                ? start.gt(cursor).or(start.eq(cursor).and(id.gt(cursorId)))
                : start.lt(cursor).or(start.eq(cursor).and(id.lt(cursorId))));
        }
        return dsl.select(id, game, owner, title, status, start, count, capacity, ready, reservationStatus)
            .from(DSL.table(DSL.name("meetup")).as("m"))
            .join(DSL.table(DSL.name("meetup_session")).as("s"))
            .on(field("s", "meetup_id", Long.class).eq(id))
            .leftJoin(DSL.table(DSL.name("reservation")).as("r"))
            .on(field("r", "session_id", Long.class).eq(field("s", "id", Long.class)))
            .and(field("r", "member_id", Long.class).eq(memberId))
            .where(condition).orderBy(ascending ? start.asc() : start.desc(), ascending ? id.asc() : id.desc())
            .limit(limit).fetch(row -> {
                boolean visible = !"DELETED".equals(row.get(status));
                return new Item(row.get(id), row.get(game), row.get(title), row.get(start).toInstant(ZoneOffset.UTC),
                    row.get(status), row.get(owner) == memberId ? "OWNER" : "JOINED", row.get(count), row.get(capacity),
                    visible, visible && !row.get(start).isBefore(utc), Boolean.TRUE.equals(row.get(ready)),
                    visible && row.get(start).isAfter(utc) && "CONFIRMED".equals(row.get(reservationStatus)));
            });
    }
    private static <T> Field<T> field(String alias, String name, Class<T> type) {
        return DSL.field(DSL.name(alias, name), type);
    }
}
