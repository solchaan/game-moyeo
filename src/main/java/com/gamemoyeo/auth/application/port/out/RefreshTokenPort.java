package com.gamemoyeo.auth.application.port.out;

import java.time.Instant;

public interface RefreshTokenPort {

    void save(
        String id,
        long memberId,
        String tokenHash,
        String tokenFamily,
        Instant issuedAt,
        Instant expiresAt
    );
}
