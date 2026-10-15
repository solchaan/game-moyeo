package com.gamemoyeo.member.application.port.in;

import com.gamemoyeo.member.domain.PositionPreferences;

public interface GameProfileUseCase {
    PositionPreferences find(long memberId, long gameId);
    PositionPreferences save(long memberId, long gameId, PositionPreferences preferences);
    String saveNickname(long memberId, long gameId, String nickname);
    java.util.Map<Long, String> nicknames(long gameId, java.util.List<Long> memberIds);
}
