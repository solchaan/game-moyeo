package com.gamemoyeo.reservation.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.application.port.in.PositionSelection;
import com.gamemoyeo.reservation.application.port.out.ReservationPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ParticipationPreferencesService {
    private final ReservationPort port;
    private final GameCatalogUseCase games;
    private final GameProfileUseCase profiles;
    public ParticipationPreferencesService(ReservationPort port, GameCatalogUseCase games, GameProfileUseCase profiles) {
        this.port = port;
        this.games = games;
        this.profiles = profiles;
    }
    public PreferencesView defaults(long meetupId, long memberId) {
        var context = port.positionContext(meetupId);
        var game = games.findGame(context.gameId());
        boolean enabled = game.positionsAllowed(context.modeOptionId(), context.mapOptionId());
        return new PreferencesView(enabled, PositionSelection.from(enabled ? profiles.find(memberId, context.gameId()) : PositionPreferences.empty()));
    }
    public PositionPreferences resolve(long meetupId, long memberId, PositionPreferences requested) {
        var defaults = defaults(meetupId, memberId);
        if (!defaults.enabled()) {
            if (requested != null && !requested.equals(PositionPreferences.empty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "POSITIONS_NOT_ALLOWED", "이 모임에서는 포지션을 선택할 수 없습니다.");
            }
            return PositionPreferences.empty();
        }
        return requested == null ? defaults.positions().toPreferences() : requested;
    }
    public record PreferencesView(boolean enabled, PositionSelection positions) { }
}
