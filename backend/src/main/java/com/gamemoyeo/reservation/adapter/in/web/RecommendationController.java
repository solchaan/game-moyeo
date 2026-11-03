package com.gamemoyeo.reservation.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.RecommendationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {
    private final RecommendationUseCase useCase;
    public RecommendationController(RecommendationUseCase useCase) { this.useCase = useCase; }
    @Operation(summary = "내 포지션과 조건에 맞는 모임", description = "주 포지션 공석, 부 포지션 공석, 조건 일치 순. 동일 점수는 시작 시간과 ID 순.")
    @GetMapping("/api/v1/members/me/recommendations")
    RecommendationUseCase.Page find(@AuthenticationPrincipal Jwt jwt, @RequestParam long gameId,
        @RequestParam(required=false) Long regionId, @RequestParam(required=false) Long modeId,
        @RequestParam(required=false) Long tierId, @RequestParam(required=false) String playStyle,
        @RequestParam(required=false) Instant from, @RequestParam(required=false) Instant to,
        @RequestParam(required=false) String cursor, @RequestParam(defaultValue="20") int size) {
        if (jwt == null) { throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Authentication is required."); }
        long member;
        try { member = Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException error) { throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_AUTHENTICATION_SUBJECT","Invalid subject."); }
        return useCase.find(member,new RecommendationUseCase.Query(gameId,regionId,modeId,tierId,playStyle,from,to,cursor,size));
    }
}
