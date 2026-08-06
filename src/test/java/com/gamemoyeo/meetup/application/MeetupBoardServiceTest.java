package com.gamemoyeo.meetup.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameDetailView;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameView;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionView;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.port.out.MeetupBoardPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeetupBoardServiceTest {

    private MeetupBoardPort port;
    private GameCatalogUseCase catalog;
    private MeetupBoardService service;

    @BeforeEach
    void setUp() {
        port = mock(MeetupBoardPort.class);
        catalog = mock(GameCatalogUseCase.class);
        service = new MeetupBoardService(port, catalog);
    }

    @Test
    void createsMeetupWithOptionsBelongingToSelectedGame() {
        MeetupCommand command = command(10L);
        when(catalog.findGame(1L)).thenReturn(game(10L, OptionType.MODE));

        service.create(7L, command);

        verify(port).create(7L, command);
    }

    @Test
    void rejectsOptionOfWrongType() {
        MeetupCommand command = command(10L);
        when(catalog.findGame(1L)).thenReturn(game(10L, OptionType.TIER));

        assertThatThrownBy(() -> service.create(7L, command))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid or inactive game option");
    }

    private MeetupCommand command(long modeOptionId) {
        Instant start = Instant.now().plusSeconds(3600);
        return new MeetupCommand(1L, modeOptionId, null, null, null, null, "Rank game", null,
            "COMPETITIVE", "REQUIRED", "FIRST_COME", start.minusSeconds(600), start,
            start.plusSeconds(7200), 5, Map.of());
    }

    private GameDetailView game(long optionId, OptionType type) {
        GameView game = new GameView(1L, "test-game", "Test Game", null, null, true);
        OptionView option = new OptionView(optionId, 1L, type, "RANKED", "Ranked", 0, true, null);
        return new GameDetailView(game, List.of(option));
    }
}
