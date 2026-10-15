package com.gamemoyeo.game.application;

import java.util.List;

public interface GameCatalogUseCase {

    GameDetailView createGame(GameCommand command, List<OptionCommand> options);

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
        @com.fasterxml.jackson.annotation.JsonProperty("roleSelectionAllowed")
        @io.swagger.v3.oas.annotations.media.Schema(description = "Whether this active mode (or legacy map) allows position requirements.")
        public boolean roleSelectionAllowed() {
            return active && (type == OptionType.MAP && "SUMMONERS_RIFT".equals(code)
                || type == OptionType.MODE && !java.util.Set.of("ARAM", "ARAM_MEYHEM", "ARAM_MAYHEM").contains(code));
        }
    }

    record GameDetailView(GameView game, List<OptionView> options) {
        public boolean positionsAllowed(Long modeId, Long legacyMapId) {
            if (!"league-of-legends".equals(game.slug())) {
                return false;
            }
            return options.stream().anyMatch(option -> java.util.Objects.equals(option.id(), modeId != null ? modeId : legacyMapId)
                && option.type() == (modeId != null ? OptionType.MODE : OptionType.MAP) && option.roleSelectionAllowed());
        }
    }
}
