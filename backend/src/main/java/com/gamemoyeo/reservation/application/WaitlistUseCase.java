package com.gamemoyeo.reservation.application;

import java.time.Instant;

public interface WaitlistUseCase {
    View find(long meetupId, long memberId);
    View join(long meetupId, long memberId);
    View cancel(long meetupId, long memberId);
    void advance();
    @io.swagger.v3.oas.annotations.media.Schema(name = "WaitlistView")
    record View(String status, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Integer position, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Instant offeredUntil, boolean canJoin, boolean canAccept, boolean canCancel) { }
}
