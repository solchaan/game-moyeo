package com.gamemoyeo.notification.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.notification.application.NotificationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationUseCase useCase;
    public NotificationController(NotificationUseCase useCase) { this.useCase = useCase; }
    @Operation(summary = "내 알림 목록")
    @GetMapping("/api/v1/members/me/notifications")
    NotificationUseCase.Page find(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "20") int size) {
        return useCase.find(memberId(jwt), cursor, size);
    }
    @Operation(summary = "내 알림 읽음 처리")
    @PutMapping("/api/v1/members/me/notifications/{id}/read")
    ResponseEntity<Void> read(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        useCase.read(memberId(jwt), id);
        return ResponseEntity.noContent().build();
    }
    private long memberId(Jwt jwt) {
        if (jwt == null) { throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required."); }
        try { return Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException error) { throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION_SUBJECT", "Invalid subject."); }
    }
}
