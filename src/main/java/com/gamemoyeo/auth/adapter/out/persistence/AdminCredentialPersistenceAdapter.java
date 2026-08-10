package com.gamemoyeo.auth.adapter.out.persistence;

import com.gamemoyeo.auth.application.port.out.AdminCredentialPort;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.adapter.out.persistence.MemberJpaEntity;
import com.gamemoyeo.member.adapter.out.persistence.MemberRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AdminCredentialPersistenceAdapter implements AdminCredentialPort {

    private final AdminCredentialRepository credentials;
    private final MemberRepository members;

    public AdminCredentialPersistenceAdapter(AdminCredentialRepository credentials, MemberRepository members) {
        this.credentials = credentials;
        this.members = members;
    }

    @Override
    public Optional<Credential> find(String username) {
        return credentials.findByUsername(username).map(entity -> new Credential(
            entity.getId(), entity.getMember().getId(), entity.getPasswordHash(), entity.getFailedAttempts(),
            entity.getLockedUntil(), entity.getMember().getStatus(), entity.getMember().getRole()));
    }

    @Override
    public void recordFailure(long credentialId, int failedAttempts, Instant lockedUntil) {
        credential(credentialId).recordFailure(failedAttempts, lockedUntil);
    }

    @Override
    public void recordSuccess(long credentialId, Instant loginAt) {
        credential(credentialId).recordSuccess(loginAt);
    }

    @Override
    public void create(String username, String passwordHash) {
        MemberJpaEntity member = members.save(MemberJpaEntity.localAdmin(username));
        credentials.save(new AdminCredentialJpaEntity(member, username, passwordHash, Instant.now()));
    }

    private AdminCredentialJpaEntity credential(long id) {
        return credentials.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.UNAUTHORIZED, "INVALID_ADMIN_CREDENTIALS", "Username or password is incorrect."));
    }
}
