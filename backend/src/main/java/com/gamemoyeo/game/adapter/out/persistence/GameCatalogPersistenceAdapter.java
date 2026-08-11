package com.gamemoyeo.game.adapter.out.persistence;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameDetailView;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameView;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionView;
import com.gamemoyeo.game.application.port.out.GameCatalogPort;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GameCatalogPersistenceAdapter implements GameCatalogPort {

    private final GameRepository games;
    private final GameOptionRepository options;

    public GameCatalogPersistenceAdapter(GameRepository games, GameOptionRepository options) {
        this.games = games;
        this.options = options;
    }

    @Override
    public GameView createGame(GameCommand command) {
        return view(games.save(new GameJpaEntity(
            command.slug(), command.name(), command.description(), command.imageUrl())));
    }

    @Override
    public GameView updateGame(long gameId, GameCommand command) {
        GameJpaEntity game = game(gameId);
        game.update(command.slug(), command.name(), command.description(), command.imageUrl());
        return view(game);
    }

    @Override
    public void deactivateGame(long gameId) {
        game(gameId).status = "INACTIVE";
    }

    @Override
    public OptionView createOption(long gameId, OptionCommand command) {
        if (options.existsByGameIdAndTypeAndCode(gameId, command.type(), command.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "GAME_OPTION_DUPLICATED", "Game option already exists.");
        }
        return view(options.save(new GameOptionJpaEntity(game(gameId), command.type(), command.code(),
            command.displayName(), command.sortOrder(), command.metadata())));
    }

    @Override
    public OptionView updateOption(long gameId, long optionId, OptionCommand command) {
        GameOptionJpaEntity option = option(gameId, optionId);
        if (option.type != command.type() || !option.code.equals(command.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "GAME_OPTION_IDENTITY_IMMUTABLE",
                "Option type and code cannot be changed; deactivate it and create a new option.");
        }
        option.update(command.type(), command.code(), command.displayName(), command.sortOrder(), command.metadata());
        return view(option);
    }

    @Override
    public void deactivateOption(long gameId, long optionId) {
        option(gameId, optionId).active = false;
    }

    @Override
    public List<GameView> findActiveGames() {
        return games.findAllByStatusOrderByNameAsc("ACTIVE").stream().map(this::view).toList();
    }

    @Override
    public GameDetailView findGame(long gameId) {
        GameJpaEntity game = game(gameId);
        return new GameDetailView(view(game), options.findAllByGameIdAndActiveTrueOrderByTypeAscSortOrderAscIdAsc(gameId)
            .stream().map(this::view).toList());
    }

    private GameJpaEntity game(long id) {
        return games.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "GAME_NOT_FOUND", "Game was not found."));
    }

    private GameOptionJpaEntity option(long gameId, long optionId) {
        GameOptionJpaEntity option = options.findById(optionId).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "GAME_OPTION_NOT_FOUND", "Game option was not found."));
        if (!option.game.id.equals(gameId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "GAME_OPTION_NOT_FOUND", "Game option was not found.");
        }
        return option;
    }

    private GameView view(GameJpaEntity game) {
        return new GameView(game.id, game.slug, game.name, game.description, game.imageUrl,
            "ACTIVE".equals(game.status));
    }

    private OptionView view(GameOptionJpaEntity option) {
        return new OptionView(option.id, option.game.id, option.type, option.code, option.displayName,
            option.sortOrder, option.active, option.metadata);
    }
}
