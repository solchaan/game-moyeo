package com.gamemoyeo.member.application.service;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.application.port.out.GameProfilePort;
import com.gamemoyeo.member.domain.PositionPreferences;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GameProfileService implements GameProfileUseCase {
    private final GameProfilePort port;
    private final GameCatalogUseCase games;
    public GameProfileService(GameProfilePort port, GameCatalogUseCase games) {
        this.port = port;
        this.games = games;
    }
    public PositionPreferences find(long memberId, long gameId) {
        requireLeague(gameId);
        return port.find(memberId, gameId);
    }
    @Transactional
    public PositionPreferences save(long memberId, long gameId, PositionPreferences preferences) {
        requireLeague(gameId);
        port.save(memberId, gameId, preferences);
        return preferences;
    }
    private void requireLeague(long gameId) {
        if (!"league-of-legends".equals(games.findGame(gameId).game().slug())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_GAME_PROFILE", "롤 프로필만 설정할 수 있습니다.");
        }
    }
}
