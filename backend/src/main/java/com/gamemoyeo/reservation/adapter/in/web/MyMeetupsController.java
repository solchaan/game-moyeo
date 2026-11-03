package com.gamemoyeo.reservation.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Filter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyMeetupsController {
    private final MyMeetupsUseCase useCase;
    public MyMeetupsController(MyMeetupsUseCase useCase) { this.useCase = useCase; }

    @Operation(summary = "내 모임", description = "UPCOMING: 참여 예정(시작순), HOSTED: 내가 만든 모임, PAST: 시작 시간이 지난 모임(최근 시작순). cursor는 응답 값을 그대로 전달합니다.")
    @GetMapping("/api/v1/members/me/meetups")
    MyMeetupsUseCase.Page find(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "UPCOMING") Filter filter,
        @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size) {
        if (jwt == null) { throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required."); }
        long memberId;
        try { memberId = Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException exception) { throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION_SUBJECT", "Invalid subject."); }
        return useCase.find(memberId, filter, cursor, size);
    }
}
