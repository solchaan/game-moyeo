package com.gamemoyeo.auth.application;

import com.gamemoyeo.auth.application.port.out.AdminCredentialPort;
import com.gamemoyeo.auth.application.port.out.AdminCredentialPort.Credential;
import com.gamemoyeo.common.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthenticationService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AdminCredentialPort credentialPort;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AdminAuthenticationService(AdminCredentialPort credentialPort, PasswordEncoder passwordEncoder) {
        this.credentialPort = credentialPort;
        this.passwordEncoder = passwordEncoder;
        this.clock = Clock.systemUTC();
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-admin-password");
    }

    @Transactional
    public long authenticate(String username, String password) {
        String normalizedUsername = normalize(username);
        Credential credential = credentialPort.find(normalizedUsername).orElse(null);
        if (credential == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw invalidCredentials();
        }

        Instant now = Instant.now(clock);
        if (credential.lockedUntil() != null && credential.lockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.LOCKED, "ADMIN_ACCOUNT_LOCKED",
                "Admin account is temporarily locked. Try again later.");
        }
        if (!"ACTIVE".equals(credential.memberStatus()) || !"ADMIN".equals(credential.memberRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_ACCOUNT_DISABLED",
                "Admin account is not active.");
        }
        if (!passwordEncoder.matches(password, credential.passwordHash())) {
            int failures = credential.failedAttempts() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            credentialPort.recordFailure(credential.id(), failures, lockedUntil);
            throw invalidCredentials();
        }

        credentialPort.recordSuccess(credential.id(), now);
        return credential.memberId();
    }

    @Transactional
    public void provisionLocalAdmin(String username, String rawPassword) {
        String normalizedUsername = normalize(username);
        if (credentialPort.find(normalizedUsername).isPresent()) {
            return;
        }
        credentialPort.create(normalizedUsername, passwordEncoder.encode(rawPassword));
    }

    private String normalize(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ADMIN_CREDENTIALS",
            "Username or password is incorrect.");
    }
}
