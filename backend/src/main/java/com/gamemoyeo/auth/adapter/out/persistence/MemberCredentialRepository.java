package com.gamemoyeo.auth.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCredentialRepository extends JpaRepository<MemberCredentialJpaEntity, Long> {

    Optional<MemberCredentialJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
