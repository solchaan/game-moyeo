package com.gamemoyeo.game.adapter.out.persistence;

import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface GameRepository extends JpaRepository<GameJpaEntity, Long> {
    List<GameJpaEntity> findAllByStatusOrderByNameAsc(String status);
}

interface GameOptionRepository extends JpaRepository<GameOptionJpaEntity, Long> {
    List<GameOptionJpaEntity> findAllByGameIdAndActiveTrueOrderByTypeAscSortOrderAscIdAsc(long gameId);
    boolean existsByGameIdAndTypeAndCode(long gameId, OptionType type, String code);
}
