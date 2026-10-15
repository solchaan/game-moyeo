package com.gamemoyeo.reservation.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import com.gamemoyeo.reservation.application.port.out.ReservationPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParticipationPreferencesServiceTest {
    private final ReservationPort port = mock(ReservationPort.class);
    private final GameCatalogUseCase games = mock(GameCatalogUseCase.class);
    private final GameProfileUseCase profiles = mock(GameProfileUseCase.class);
    private final ParticipationPreferencesService service = new ParticipationPreferencesService(port, games, profiles);
    private void game(String map) {
        when(port.positionContext(10)).thenReturn(new ReservationPort.PositionContext(1, 2L));
        when(games.findGame(1)).thenReturn(new GameCatalogUseCase.GameDetailView(
            new GameCatalogUseCase.GameView(1, "league-of-legends", "League", null, null, true),
            List.of(new GameCatalogUseCase.OptionView(2, 1, GameCatalogUseCase.OptionType.MAP, map, map, 1, true, null))));
    }
    @Test
    void defaultsToProfileAndAcceptsOverrideWithoutSavingProfile() {
        game("SUMMONERS_RIFT");
        var profile = new PositionPreferences(Position.MID, Position.SUPPORT);
        var override = new PositionPreferences(Position.JUNGLE, Position.TOP);
        when(profiles.find(7, 1)).thenReturn(profile);
        assertThat(service.resolve(10, 7, null)).isEqualTo(profile);
        assertThat(service.resolve(10, 7, override)).isEqualTo(override);
        assertThat(service.resolve(10, 7, PositionPreferences.empty())).isEqualTo(PositionPreferences.empty());
        verify(profiles, never()).save(anyLong(), anyLong(), any());
    }
    @Test
    void aramDoesNotApplyProfileAndRejectsPositions() {
        game("HOWLING_ABYSS");
        assertThat(service.defaults(10, 7).enabled()).isFalse();
        assertThat(service.resolve(10, 7, null)).isEqualTo(PositionPreferences.empty());
        assertThatThrownBy(() -> service.resolve(10, 7, new PositionPreferences(Position.MID, null)))
            .isInstanceOf(ApiException.class);
        verifyNoInteractions(profiles);
    }
}
