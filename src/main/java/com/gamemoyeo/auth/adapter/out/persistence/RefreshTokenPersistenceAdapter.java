package com.gamemoyeo.auth.adapter.out.persistence;

import com.gamemoyeo.auth.application.port.out.RefreshTokenPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository repository;

    public RefreshTokenPersistenceAdapter(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(
        String id,
        long memberId,
        String tokenHash,
        String tokenFamily,
        Instant issuedAt,
        Instant expiresAt
    ) {
        repository.save(new RefreshTokenJpaEntity(
            id, memberId, tokenHash, tokenFamily, issuedAt, expiresAt));
    }
}
