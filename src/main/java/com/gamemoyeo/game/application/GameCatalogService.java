package com.gamemoyeo.game.application;

import com.gamemoyeo.game.application.port.out.GameCatalogPort;
import java.util.List;
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
    public GameView createGame(GameCommand command) {
        return port.createGame(command);
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
}
