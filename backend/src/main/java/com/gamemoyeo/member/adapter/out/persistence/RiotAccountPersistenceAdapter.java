package com.gamemoyeo.member.adapter.out.persistence;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.out.RiotAccountPort;
import com.gamemoyeo.member.domain.RiotAccount;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RiotAccountPersistenceAdapter implements RiotAccountPort {
    private final RiotAccountRepository accounts;
    private final MemberRepository members;
    public RiotAccountPersistenceAdapter(RiotAccountRepository accounts, MemberRepository members) {
        this.accounts = accounts;
        this.members = members;
    }
    @Override
    public Optional<RiotAccount> find(long memberId) {
        return accounts.findById(memberId).map(RiotAccountJpaEntity::toDomain);
    }
    @Override
    public void link(long memberId, RiotAccount account) {
        var member = members.findForAccountLink(memberId);
        if (member.isEmpty() || !"ACTIVE".equals(member.get().getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_INACTIVE", "활성 회원만 계정을 연결할 수 있습니다.");
        }
        var existing = find(memberId);
        if (existing.isPresent() && !existing.get().puuid().equals(account.puuid())) {
            throw conflict();
        }
        try {
            accounts.saveAndFlush(new RiotAccountJpaEntity(memberId, account));
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
    }
    @Override
    public void unlink(long memberId) {
        members.findForAccountLink(memberId);
        accounts.deleteById(memberId);
    }
    private ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "RIOT_ACCOUNT_ALREADY_LINKED", "이미 연결된 계정입니다. 기존 연결을 확인해 주세요.");
    }
}
