package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.domain.PositionPreferences;

public interface GameProfilePort {
    PositionPreferences find(long memberId, long gameId);
    void save(long memberId, long gameId, PositionPreferences preferences);
    void saveNickname(long memberId, long gameId, String nickname);
    java.util.Map<Long, String> nicknames(long gameId, java.util.List<Long> memberIds);
}
