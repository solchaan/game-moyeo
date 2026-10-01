package com.gamemoyeo.meetup.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupView;
import com.gamemoyeo.meetup.application.port.out.MeetupBoardPort;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeetupBoardServiceTest {

    private MeetupBoardPort port;
    private GameCatalogUseCase catalog;
    private ReservationUseCase reservation;
    private MeetupBoardService service;

    @BeforeEach
    void setUp() {
        port = mock(MeetupBoardPort.class);
        catalog = mock(GameCatalogUseCase.class);
        reservation = mock(ReservationUseCase.class);
        service = new MeetupBoardService(port, catalog, reservation);
    }

    @Test
    void createsMeetupWithOptionsBelongingToSelectedGame() {
        MeetupCommand command = command(10L);
        when(catalog.findGame(1L)).thenReturn(game(10L, OptionType.MODE));
        when(port.create(7L, command)).thenReturn(view());

        service.create(7L, command);

        verify(port).create(7L, command);
        verify(reservation).initializeOwner(99L, 7L);
    }

    @Test
    void rejectsOptionOfWrongType() {
        MeetupCommand command = command(10L);
        when(catalog.findGame(1L)).thenReturn(game(10L, OptionType.TIER));

        assertThatThrownBy(() -> service.create(7L, command))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid or inactive game option")
            .satisfies(error -> assertThat(((ApiException) error).fieldErrors()).containsKey("modeOptionId"));
    }

    @Test
    void rejectsMinimumTierHigherThanMaximumTier() {
        MeetupCommand command = tierCommand(20L, 10L);
        GameView game = new GameView(1L, "test-game", "Test Game", null, null, true);
        OptionView bronze = new OptionView(10L, 1L, OptionType.TIER, "BRONZE", "Bronze", 10, true, null);
        OptionView gold = new OptionView(20L, 1L, OptionType.TIER, "GOLD", "Gold", 20, true, null);
        when(catalog.findGame(1L)).thenReturn(new GameDetailView(game, List.of(bronze, gold)));

        assertThatThrownBy(() -> service.create(7L, command))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Minimum tier cannot be higher")
            .satisfies(error -> assertThat(((ApiException) error).fieldErrors()).containsKey("minimumTierOptionId"));
    }

    @Test
    void imminentStartIdentifiesStartField() {
        Instant start = Instant.now().plusSeconds(300);
        MeetupCommand command = new MeetupCommand(1L, null, null, null, null, null,
            "Test", null, "CASUAL", "OPTIONAL", "FIRST_COME", null, start,
            start.plusSeconds(3600), 5, Map.of());
        assertThatThrownBy(() -> service.create(7L, command))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> assertThat(((ApiException) error).fieldErrors()).containsKey("startsAt"));
    }

    @Test
    void endBeforeStartIdentifiesEndField() {
        Instant start = Instant.now().plusSeconds(3600);
        MeetupCommand command = new MeetupCommand(1L, null, null, null, null, null,
            "Test", null, "CASUAL", "OPTIONAL", "FIRST_COME", null, start,
            start.minusSeconds(300), 5, Map.of());
        assertThatThrownBy(() -> service.create(7L, command))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> assertThat(((ApiException) error).fieldErrors()).containsKey("endsAt"));
    }

    @Test
    void filtersByValidatedServerAndRetainsCursorPagination() {
        when(catalog.findGame(1L)).thenReturn(game(10L, OptionType.REGION));
        when(port.findAll(1L, 10L, 200L, 2)).thenReturn(List.of(view(), view()));
        var page = service.findAll(1L, 10L, 200L, 1);
        assertThat(page.items()).hasSize(1);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(99L);
        verify(port).findAll(1L, 10L, 200L, 2);
    }

    @Test
    void rejectsServerWithoutGameOrFromAnotherGame() {
        assertThatThrownBy(() -> service.findAll(null, 10L, null, 20))
            .isInstanceOf(ApiException.class);
        when(catalog.findGame(1L)).thenReturn(game(11L, OptionType.REGION));
        assertThatThrownBy(() -> service.findAll(1L, 10L, null, 20))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> assertThat(((ApiException) error).fieldErrors()).containsKey("regionOptionId"));
    }

    @Test
    void preservesUnfilteredListingWithoutServer() {
        when(port.findAll(null, null, null, 21)).thenReturn(List.of(view()));
        var page = service.findAll(null, null, null, 20);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.items()).hasSize(1);
    }

    private MeetupCommand command(long modeOptionId) {
        Instant start = Instant.now().plusSeconds(3600);
        return new MeetupCommand(1L, modeOptionId, null, null, null, null, "Rank game", null,
            "COMPETITIVE", "REQUIRED", "FIRST_COME", start.minusSeconds(600), start,
            start.plusSeconds(7200), 5, Map.of());
    }

    private MeetupCommand tierCommand(long minimumTierOptionId, long maximumTierOptionId) {
        Instant start = Instant.now().plusSeconds(3600);
        return new MeetupCommand(1L, null, null, null, minimumTierOptionId, maximumTierOptionId,
            "Rank game", null, "COMPETITIVE", "REQUIRED", "FIRST_COME", start.minusSeconds(600),
            start, start.plusSeconds(7200), 5, Map.of());
    }

    private GameDetailView game(long optionId, OptionType type) {
        GameView game = new GameView(1L, "test-game", "Test Game", null, null, true);
        OptionView option = new OptionView(optionId, 1L, type, "RANKED", "Ranked", 0, true, null);
        return new GameDetailView(game, List.of(option));
    }

    private MeetupView view() {
        Instant start = Instant.now().plusSeconds(3600);
        return new MeetupView(99L, 1L, 7L, "Rank game", null, null, null, null, null, null,
            "COMPETITIVE", "REQUIRED", "FIRST_COME", start.minusSeconds(600), start,
            start.plusSeconds(7200), 5, 1, "OPEN", Map.of());
    }
}
