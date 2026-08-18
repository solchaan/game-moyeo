package com.gamemoyeo.auth.application;

import com.gamemoyeo.auth.application.port.out.MemberCredentialPort;
import com.gamemoyeo.auth.application.port.out.MemberCredentialPort.Credential;
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
public class MemberAuthenticationService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final MemberCredentialPort credentialPort;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock = Clock.systemUTC();
    private final String dummyPasswordHash;

    public MemberAuthenticationService(MemberCredentialPort credentialPort, PasswordEncoder passwordEncoder) {
        this.credentialPort = credentialPort;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-member-password");
    }

    @Transactional
    public long register(String username, String password, String nickname, String email) {
        String normalizedUsername = normalize(username);
        String normalizedNickname = nickname.strip();
        if (credentialPort.usernameExists(normalizedUsername)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS",
                "Username is already in use.");
        }
        if (credentialPort.nicknameExists(normalizedNickname)) {
            throw new ApiException(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS",
                "Nickname is already in use.");
        }
        String normalizedEmail = email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
        return credentialPort.create(normalizedUsername, passwordEncoder.encode(password),
            normalizedNickname, normalizedEmail, Instant.now(clock));
    }

    @Transactional
    public long authenticate(String username, String password) {
        Credential credential = credentialPort.find(normalize(username)).orElse(null);
        if (credential == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw invalidCredentials();
        }
        Instant now = Instant.now(clock);
        if (credential.lockedUntil() != null && credential.lockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.LOCKED, "MEMBER_ACCOUNT_LOCKED",
                "Account is temporarily locked. Try again later.");
        }
        if (!"ACTIVE".equals(credential.memberStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_ACCOUNT_DISABLED",
                "Account is not active.");
        }
        if (!passwordEncoder.matches(password, credential.passwordHash())) {
            int failures = credential.failedAttempts() + 1;
            credentialPort.recordFailure(credential.id(), failures,
                failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null);
            throw invalidCredentials();
        }
        credentialPort.recordSuccess(credential.id(), now);
        return credential.memberId();
    }

    private String normalize(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_MEMBER_CREDENTIALS",
            "Username or password is incorrect.");
    }
}
