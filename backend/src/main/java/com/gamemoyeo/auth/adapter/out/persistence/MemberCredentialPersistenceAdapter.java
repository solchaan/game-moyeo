package com.gamemoyeo.auth.adapter.out.persistence;

import com.gamemoyeo.auth.application.port.out.MemberCredentialPort;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.adapter.out.persistence.MemberJpaEntity;
import com.gamemoyeo.member.adapter.out.persistence.MemberRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MemberCredentialPersistenceAdapter implements MemberCredentialPort {

    private final MemberCredentialRepository credentials;
    private final MemberRepository members;

    public MemberCredentialPersistenceAdapter(
        MemberCredentialRepository credentials,
        MemberRepository members
    ) {
        this.credentials = credentials;
        this.members = members;
    }

    @Override
    public Optional<Credential> find(String username) {
        return credentials.findByUsername(username).map(entity -> new Credential(
            entity.getId(), entity.getMember().getId(), entity.getPasswordHash(),
            entity.getFailedAttempts(), entity.getLockedUntil(), entity.getMember().getStatus()));
    }

    @Override
    public boolean usernameExists(String username) {
        return credentials.existsByUsername(username);
    }

    @Override
    public boolean nicknameExists(String nickname) {
        return members.existsByNickname(nickname);
    }

    @Override
    public long create(String username, String passwordHash, String nickname, String email, Instant now) {
        try {
            MemberJpaEntity member = members.save(new MemberJpaEntity(nickname, email, null));
            credentials.save(new MemberCredentialJpaEntity(member, username, passwordHash, now));
            return member.getId();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMBER_ACCOUNT_ALREADY_EXISTS",
                "Username or nickname is already in use.");
        }
    }

    @Override
    public void recordFailure(long credentialId, int failedAttempts, Instant lockedUntil) {
        credential(credentialId).recordFailure(failedAttempts, lockedUntil);
    }

    @Override
    public void recordSuccess(long credentialId, Instant loginAt) {
        credential(credentialId).recordSuccess(loginAt);
    }

    private MemberCredentialJpaEntity credential(long id) {
        return credentials.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.UNAUTHORIZED, "INVALID_MEMBER_CREDENTIALS", "Username or password is incorrect."));
    }
}
