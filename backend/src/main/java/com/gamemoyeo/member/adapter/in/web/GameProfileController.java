package com.gamemoyeo.member.adapter.in.web;

import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.application.port.in.PositionSelection;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/members/me/games/{gameId}/profile")
public class GameProfileController {
    private final GameProfileUseCase profiles;
    public GameProfileController(GameProfileUseCase profiles) { this.profiles = profiles; }
    private long memberId(Jwt jwt) {
        if (jwt == null) {
            throw new com.gamemoyeo.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
        }
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new com.gamemoyeo.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "INVALID_AUTHENTICATION_SUBJECT", "Authentication subject is invalid.");
        }
    }
    @GetMapping
    public PositionSelection find(@AuthenticationPrincipal Jwt jwt, @PathVariable long gameId) {
        return PositionSelection.from(profiles.find(memberId(jwt), gameId));
    }
    @PutMapping
    public PositionSelection save(@AuthenticationPrincipal Jwt jwt, @PathVariable long gameId,
        @RequestBody PositionSelection preferences) {
        return PositionSelection.from(profiles.save(memberId(jwt), gameId, preferences.toPreferences()));
    }
}
