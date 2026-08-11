package com.gamemoyeo.member.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentityJpaEntity, Long> {

    Optional<SocialIdentityJpaEntity> findByProviderAndProviderSubject(String provider, String providerSubject);
}
