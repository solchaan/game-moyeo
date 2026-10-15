package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.domain.PositionPreferences;

public interface GameProfilePort {
    PositionPreferences find(long memberId, long gameId);
    void save(long memberId, long gameId, PositionPreferences preferences);
}
