package com.gamemoyeo.auth.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface AdminCredentialPort {

    Optional<Credential> find(String username);

    void recordFailure(long credentialId, int failedAttempts, Instant lockedUntil);

    void recordSuccess(long credentialId, Instant loginAt);

    void create(String username, String passwordHash);

    record Credential(
        long id,
        long memberId,
        String passwordHash,
        int failedAttempts,
        Instant lockedUntil,
        String memberStatus,
        String memberRole
    ) {
    }
}
