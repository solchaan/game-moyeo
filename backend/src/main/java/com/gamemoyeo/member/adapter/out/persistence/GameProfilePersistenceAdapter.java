package com.gamemoyeo.member.adapter.out.persistence;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.out.GameProfilePort;
import com.gamemoyeo.member.domain.PositionPreferences;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GameProfilePersistenceAdapter implements GameProfilePort {
    private final GameProfileRepository profiles;
    private final MemberRepository members;
    private final org.jooq.DSLContext dsl;
    public GameProfilePersistenceAdapter(GameProfileRepository profiles, MemberRepository members, org.jooq.DSLContext dsl) {
        this.profiles = profiles;
        this.members = members;
        this.dsl = dsl;
    }
    public java.util.Map<Long, String> nicknames(long gameId, java.util.List<Long> memberIds) {
        var member = org.jooq.impl.DSL.field("member_id", Long.class);
        var nickname = org.jooq.impl.DSL.field("manual_nickname", String.class);
        return dsl.select(member, nickname).from("member_game_profile")
            .where(org.jooq.impl.DSL.field("game_id", Long.class).eq(gameId))
            .and(member.in(memberIds)).and(nickname.isNotNull()).fetchMap(member, nickname);
    }
    public void saveNickname(long memberId, long gameId, String nickname) {
        members.findForAccountLink(memberId).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member was not found."));
        var entity = profiles.findByMemberIdAndGameId(memberId, gameId).orElseGet(GameProfileJpaEntity::new);
        entity.memberId = memberId;
        entity.gameId = gameId;
        entity.manualNickname = nickname;
        profiles.save(entity);
    }
    public PositionPreferences find(long memberId, long gameId) {
        return profiles.findByMemberIdAndGameId(memberId, gameId)
            .map(entity -> new PositionPreferences(entity.primaryPosition, entity.secondaryPosition))
            .orElse(PositionPreferences.empty());
    }
    public void save(long memberId, long gameId, PositionPreferences value) {
        // Serialize creation of the first profile as well as subsequent updates.
        members.findForAccountLink(memberId).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member was not found."));
        var entity = profiles.findByMemberIdAndGameId(memberId, gameId).orElseGet(GameProfileJpaEntity::new);
        entity.memberId = memberId;
        entity.gameId = gameId;
        entity.primaryPosition = value.primaryPosition();
        entity.secondaryPosition = value.secondaryPosition();
        profiles.save(entity);
    }
}
