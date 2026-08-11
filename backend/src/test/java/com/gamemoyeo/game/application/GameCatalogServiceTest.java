package com.gamemoyeo.game.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameDetailView;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameView;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import com.gamemoyeo.game.application.port.out.GameCatalogPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class GameCatalogServiceTest {

    private GameCatalogPort port;
    private GameCatalogService service;

    @BeforeEach
    void setUp() {
        port = mock(GameCatalogPort.class);
        service = new GameCatalogService(port);
    }

    @Test
    void createsGameAndAllOptionsInOneUseCaseCall() {
        GameCommand game = new GameCommand("league-of-legends", "League of Legends", null, null);
        OptionCommand bronze = option("BRONZE", 10);
        OptionCommand gold = option("GOLD", 20);
        GameView created = new GameView(1L, game.slug(), game.name(), null, null, true);
        GameDetailView detail = new GameDetailView(created, List.of());
        when(port.createGame(game)).thenReturn(created);
        when(port.findGame(1L)).thenReturn(detail);

        service.createGame(game, List.of(bronze, gold));

        InOrder order = inOrder(port);
        order.verify(port).createGame(game);
        order.verify(port).createOption(1L, bronze);
        order.verify(port).createOption(1L, gold);
        order.verify(port).findGame(1L);
    }

    @Test
    void rejectsDuplicatedOptionIdentityBeforeCreatingGame() {
        GameCommand game = new GameCommand("test-game", "Test Game", null, null);

        assertThatThrownBy(() -> service.createGame(game, List.of(option("GOLD", 10), option("GOLD", 20))))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("cannot be duplicated");

        verify(port, never()).createGame(game);
    }

    private OptionCommand option(String code, int sortOrder) {
        return new OptionCommand(OptionType.TIER, code, code, sortOrder, null);
    }
}
