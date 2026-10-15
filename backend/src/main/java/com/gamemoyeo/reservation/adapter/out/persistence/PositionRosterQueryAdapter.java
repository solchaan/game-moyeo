package com.gamemoyeo.reservation.adapter.out.persistence;

import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import com.gamemoyeo.reservation.application.port.out.PositionRosterPort;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public class PositionRosterQueryAdapter implements PositionRosterPort {
    private final DSLContext dsl;

    public PositionRosterQueryAdapter(DSLContext dsl) { this.dsl = dsl; }

    @Override
    public List<MemberPosition> confirmedPositions(long meetupId) {
        var member = DSL.field(DSL.name("r", "member_id"), Long.class);
        var primary = DSL.field(DSL.name("r", "primary_position"), String.class);
        var secondary = DSL.field(DSL.name("r", "secondary_position"), String.class);
        return dsl.select(member, primary, secondary)
            .from(DSL.table(DSL.name("reservation")).as("r"))
            .join(DSL.table(DSL.name("meetup_session")).as("s"))
            .on(DSL.field(DSL.name("r", "session_id"), Long.class)
                .eq(DSL.field(DSL.name("s", "id"), Long.class)))
            .where(DSL.field(DSL.name("s", "meetup_id"), Long.class).eq(meetupId))
            .and(DSL.field(DSL.name("r", "status"), String.class).eq("CONFIRMED"))
            .orderBy(member)
            .fetch(row -> new MemberPosition(row.get(member),
                new PositionPreferences(position(row.get(primary)), position(row.get(secondary)))));
    }

    private Position position(String value) { return value == null ? null : Position.valueOf(value); }
}
