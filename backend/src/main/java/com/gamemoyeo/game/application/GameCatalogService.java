package com.gamemoyeo.game.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.port.out.GameCatalogPort;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GameCatalogService implements GameCatalogUseCase {

    private final GameCatalogPort port;

    public GameCatalogService(GameCatalogPort port) {
        this.port = port;
    }

    @Override
    @Transactional
    public GameDetailView createGame(GameCommand command, List<OptionCommand> options) {
        validateNewOptions(options);
        GameView game = port.createGame(command);
        for (OptionCommand option : options) {
            port.createOption(game.id(), option);
        }
        return port.findGame(game.id());
    }

    @Override
    @Transactional
    public GameView updateGame(long gameId, GameCommand command) {
        return port.updateGame(gameId, command);
    }

    @Override
    @Transactional
    public void deactivateGame(long gameId) {
        port.deactivateGame(gameId);
    }

    @Override
    @Transactional
    public OptionView createOption(long gameId, OptionCommand command) {
        return port.createOption(gameId, command);
    }

    @Override
    @Transactional
    public OptionView updateOption(long gameId, long optionId, OptionCommand command) {
        return port.updateOption(gameId, optionId, command);
    }

    @Override
    @Transactional
    public void deactivateOption(long gameId, long optionId) {
        port.deactivateOption(gameId, optionId);
    }

    @Override
    public List<GameView> findActiveGames() {
        return port.findActiveGames();
    }

    @Override
    public GameDetailView findGame(long gameId) {
        return port.findGame(gameId);
    }

    private void validateNewOptions(List<OptionCommand> options) {
        Set<OptionIdentity> identities = new HashSet<>();
        for (OptionCommand option : options) {
            if (!identities.add(new OptionIdentity(option.type(), option.code()))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GAME_OPTION_DUPLICATED_IN_REQUEST",
                    "Game option type and code cannot be duplicated in one request.");
            }
        }
    }

    private record OptionIdentity(OptionType type, String code) {
    }
}
