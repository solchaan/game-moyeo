package com.gamemoyeo.reservation.application.port.out;

import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.reservation.application.RecommendationUseCase.Item;
import com.gamemoyeo.reservation.application.RecommendationUseCase.Query;
import java.time.Instant;
import java.util.List;

public interface RecommendationPort {
    List<Item> find(long memberId, Query query, PositionPreferences positions, Integer tierOrder, Instant now,
        Integer cursorScore, Instant cursorTime, Long cursorId, int limit);
}
