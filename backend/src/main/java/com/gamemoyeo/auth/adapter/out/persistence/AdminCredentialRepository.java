package com.gamemoyeo.auth.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCredentialRepository extends JpaRepository<AdminCredentialJpaEntity, Long> {

    Optional<AdminCredentialJpaEntity> findByUsername(String username);
}
