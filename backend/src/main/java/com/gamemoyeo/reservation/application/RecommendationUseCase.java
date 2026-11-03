package com.gamemoyeo.reservation.application;

import java.time.Instant;
import java.util.List;

public interface RecommendationUseCase {
    Page find(long memberId, Query query);
    record Query(long gameId, Long regionId, Long modeId, Long tierId, String playStyle, Instant from, Instant to, String cursor, int size) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "RecommendationItem")
    record Item(long id, String title, Instant startsAt, int remainingCapacity, String reason, int score) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "RecommendationPage")
    record Page(List<Item> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) String nextCursor, boolean hasNext) { }
}
