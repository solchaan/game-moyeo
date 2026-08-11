package com.gamemoyeo.game.application;

import java.util.List;

public interface GameCatalogUseCase {

    GameView createGame(GameCommand command);

    GameView updateGame(long gameId, GameCommand command);

    void deactivateGame(long gameId);

    OptionView createOption(long gameId, OptionCommand command);

    OptionView updateOption(long gameId, long optionId, OptionCommand command);

    void deactivateOption(long gameId, long optionId);

    List<GameView> findActiveGames();

    GameDetailView findGame(long gameId);

    record GameCommand(String slug, String name, String description, String imageUrl) {
    }

    record OptionCommand(
        OptionType type,
        String code,
        String displayName,
        int sortOrder,
        String metadata
    ) {
    }

    enum OptionType {
        MODE, TIER, ROLE, PLATFORM, REGION, MAP
    }

    record GameView(long id, String slug, String name, String description, String imageUrl, boolean active) {
    }

    record OptionView(
        long id,
        long gameId,
        OptionType type,
        String code,
        String displayName,
        int sortOrder,
        boolean active,
        String metadata
    ) {
    }

    record GameDetailView(GameView game, List<OptionView> options) {
    }
}
