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
    @Transactional
    public String saveNickname(long memberId, long gameId, String nickname) {
        requireLeague(gameId);
        String value = nickname == null || nickname.isBlank() ? null : nickname.strip();
        if (value != null && (value.length() > 64 || value.codePoints().anyMatch(Character::isISOControl))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_GAME_NICKNAME", "닉네임은 줄바꿈 없이 64자 이내로 입력해 주세요.");
        }
        port.saveNickname(memberId, gameId, value);
        return value;
    }
    public java.util.Map<Long, String> nicknames(long gameId, java.util.List<Long> memberIds) {
        requireLeague(gameId);
        return memberIds.isEmpty() ? java.util.Map.of() : port.nicknames(gameId, memberIds);
    }
    private void requireLeague(long gameId) {
        if (!"league-of-legends".equals(games.findGame(gameId).game().slug())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_GAME_PROFILE", "롤 프로필만 설정할 수 있습니다.");
        }
    }
}
