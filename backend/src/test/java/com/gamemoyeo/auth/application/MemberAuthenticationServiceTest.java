package com.gamemoyeo.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamemoyeo.auth.application.port.out.MemberCredentialPort;
import com.gamemoyeo.auth.application.port.out.MemberCredentialPort.Credential;
import com.gamemoyeo.common.exception.ApiException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MemberAuthenticationServiceTest {

    private final InMemoryCredentials credentials = new InMemoryCredentials();
    private final MemberAuthenticationService service = new MemberAuthenticationService(
        credentials, new BCryptPasswordEncoder(4));

    @Test
    void registersAndAuthenticatesMemberWithNormalizedUsername() {
        long registeredId = service.register(" New_User ", "password1", "새유저", "USER@EXAMPLE.COM");

        long authenticatedId = service.authenticate("NEW_USER", "password1");

        assertThat(authenticatedId).isEqualTo(registeredId);
        assertThat(credentials.find("new_user")).isPresent();
    }

    @Test
    void rejectsDuplicateUsername() {
        service.register("member1", "password1", "회원1", null);

        assertThatThrownBy(() -> service.register("MEMBER1", "password2", "회원2", null))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.code()).isEqualTo("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void rejectsWrongPassword() {
        service.register("member1", "password1", "회원1", null);

        assertThatThrownBy(() -> service.authenticate("member1", "wrong-password"))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.code()).isEqualTo("INVALID_MEMBER_CREDENTIALS"));
    }

    private static class InMemoryCredentials implements MemberCredentialPort {

        private final Map<String, StoredCredential> values = new HashMap<>();
        private long sequence;

        @Override
        public Optional<Credential> find(String username) {
            return Optional.ofNullable(values.get(username)).map(StoredCredential::toCredential);
        }

        @Override
        public boolean usernameExists(String username) {
            return values.containsKey(username);
        }

        @Override
        public boolean nicknameExists(String nickname) {
            return values.values().stream().anyMatch(value -> value.nickname.equals(nickname));
        }

        @Override
        public long create(String username, String passwordHash, String nickname, String email, Instant now) {
            long id = ++sequence;
            values.put(username, new StoredCredential(id, username, nickname, passwordHash));
            return id;
        }

        @Override
        public void recordFailure(long credentialId, int failedAttempts, Instant lockedUntil) {
            values.values().stream().filter(value -> value.id == credentialId).findFirst()
                .orElseThrow().recordFailure(failedAttempts, lockedUntil);
        }

        @Override
        public void recordSuccess(long credentialId, Instant loginAt) {
            values.values().stream().filter(value -> value.id == credentialId).findFirst()
                .orElseThrow().recordSuccess();
        }
    }

    private static class StoredCredential {

        private final long id;
        private final String username;
        private final String nickname;
        private final String passwordHash;
        private int failedAttempts;
        private Instant lockedUntil;

        StoredCredential(long id, String username, String nickname, String passwordHash) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.passwordHash = passwordHash;
        }

        Credential toCredential() {
            return new Credential(id, id, passwordHash, failedAttempts, lockedUntil, "ACTIVE");
        }

        void recordFailure(int failures, Instant until) {
            failedAttempts = failures;
            lockedUntil = until;
        }

        void recordSuccess() {
            failedAttempts = 0;
            lockedUntil = null;
        }
    }
}
