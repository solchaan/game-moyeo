package com.gamemoyeo.game.application.port.out;

import com.gamemoyeo.game.application.GameCatalogUseCase.GameCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameDetailView;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameView;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionView;
import java.util.List;

public interface GameCatalogPort {

    GameView createGame(GameCommand command);

    GameView updateGame(long gameId, GameCommand command);

    void deactivateGame(long gameId);

    OptionView createOption(long gameId, OptionCommand command);

    OptionView updateOption(long gameId, long optionId, OptionCommand command);

    void deactivateOption(long gameId, long optionId);

    List<GameView> findActiveGames();

    GameDetailView findGame(long gameId);
}
