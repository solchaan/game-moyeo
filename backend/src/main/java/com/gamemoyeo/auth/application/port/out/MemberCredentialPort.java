package com.gamemoyeo.auth.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface MemberCredentialPort {

    Optional<Credential> find(String username);

    boolean usernameExists(String username);

    boolean nicknameExists(String nickname);

    long create(String username, String passwordHash, String nickname, String email, Instant now);

    void recordFailure(long credentialId, int failedAttempts, Instant lockedUntil);

    void recordSuccess(long credentialId, Instant loginAt);

    record Credential(
        long id,
        long memberId,
        String passwordHash,
        int failedAttempts,
        Instant lockedUntil,
        String memberStatus
    ) {
    }
}
