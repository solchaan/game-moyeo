package com.gamemoyeo.member.adapter.out.persistence;

import com.gamemoyeo.member.application.SocialProfile;
import com.gamemoyeo.member.application.port.out.SocialAccountPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SocialAccountPersistenceAdapter implements SocialAccountPort {

    private final MemberRepository memberRepository;
    private final SocialIdentityRepository identityRepository;
    private final Clock clock;

    public SocialAccountPersistenceAdapter(
        MemberRepository memberRepository,
        SocialIdentityRepository identityRepository
    ) {
        this.memberRepository = memberRepository;
        this.identityRepository = identityRepository;
        this.clock = Clock.systemUTC();
    }

    @Override
    public Optional<MemberAccount> find(String provider, String subject) {
        return identityRepository.findByProviderAndProviderSubject(provider, subject)
            .map(SocialIdentityJpaEntity::getMember)
            .map(member -> new MemberAccount(member.getId(), member.getStatus()));
    }

    @Override
    public MemberAccount create(SocialProfile profile, String nickname) {
        MemberJpaEntity member = memberRepository.save(
            new MemberJpaEntity(nickname, profile.email(), profile.profileImageUrl()));
        identityRepository.save(new SocialIdentityJpaEntity(member, profile, Instant.now(clock)));
        return new MemberAccount(member.getId(), member.getStatus());
    }

    @Override
    public void recordLogin(String provider, String subject, SocialProfile profile) {
        SocialIdentityJpaEntity identity = identityRepository
            .findByProviderAndProviderSubject(provider, subject)
            .orElseThrow();
        identity.recordLogin(profile, Instant.now(clock));
    }
}
