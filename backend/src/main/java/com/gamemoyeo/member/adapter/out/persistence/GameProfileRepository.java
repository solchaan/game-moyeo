package com.gamemoyeo.member.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameProfileRepository extends JpaRepository<GameProfileJpaEntity, Long> {
    Optional<GameProfileJpaEntity> findByMemberIdAndGameId(long memberId, long gameId);
}
