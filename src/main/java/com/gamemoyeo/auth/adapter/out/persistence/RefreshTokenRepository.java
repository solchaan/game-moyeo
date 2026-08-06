package com.gamemoyeo.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {
}
