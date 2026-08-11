package com.gamemoyeo.member.adapter.out.persistence;

import com.gamemoyeo.auth.application.port.out.MemberAuthorityPort;
import com.gamemoyeo.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MemberAuthorityPersistenceAdapter implements MemberAuthorityPort {

    private final MemberRepository repository;

    public MemberAuthorityPersistenceAdapter(MemberRepository repository) {
        this.repository = repository;
    }

    @Override
    public String findRole(long memberId) {
        return repository.findById(memberId).map(MemberJpaEntity::getRole).orElseThrow(() ->
            new ApiException(HttpStatus.UNAUTHORIZED, "MEMBER_NOT_FOUND", "Authenticated member was not found."));
    }
}
