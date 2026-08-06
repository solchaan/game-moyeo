package com.gamemoyeo.game.adapter.out.persistence;

import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_option")
class GameOptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    GameJpaEntity game;
    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 30)
    OptionType type;
    @Column(nullable = false, length = 80)
    String code;
    @Column(name = "display_name", nullable = false, length = 120)
    String displayName;
    @Column(name = "sort_order", nullable = false)
    int sortOrder;
    @Column(nullable = false)
    boolean active = true;
    @Column(columnDefinition = "json")
    String metadata;

    protected GameOptionJpaEntity() {
    }

    GameOptionJpaEntity(GameJpaEntity game, OptionType type, String code, String name, int order, String metadata) {
        this.game = game;
        update(type, code, name, order, metadata);
    }

    void update(OptionType type, String code, String name, int order, String metadata) {
        this.type = type;
        this.code = code;
        this.displayName = name;
        this.sortOrder = order;
        this.metadata = metadata;
    }
}
