package com.gamemoyeo.reservation.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import com.gamemoyeo.reservation.application.ReservationUseCase.ParticipationView;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meetups/{meetupId}")
public class ReservationController {

    private final ReservationUseCase useCase;

    private final com.gamemoyeo.reservation.application.ParticipationPreferencesService preferences;
    public ReservationController(ReservationUseCase useCase,
        com.gamemoyeo.reservation.application.ParticipationPreferencesService preferences) {
        this.useCase = useCase;
        this.preferences = preferences;
    }
    @GetMapping("/participation/preferences")
    public com.gamemoyeo.reservation.application.ParticipationPreferencesService.PreferencesView preferences(
        @PathVariable long meetupId, @AuthenticationPrincipal Jwt jwt) {
        return preferences.defaults(meetupId, memberId(jwt));
    }

    @GetMapping("/participation")
    ParticipationView state(@PathVariable long meetupId, @AuthenticationPrincipal Jwt jwt) {
        return useCase.state(meetupId, jwt == null ? null : memberId(jwt));
    }

    @PostMapping("/reservations")
    ParticipationView participate(
        @PathVariable long meetupId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @org.springframework.web.bind.annotation.RequestBody(required = false)
        com.gamemoyeo.member.application.port.in.PositionSelection positions
    ) {
        return positions == null ? useCase.participate(meetupId, memberId(jwt), idempotencyKey)
            : useCase.participate(meetupId, memberId(jwt), idempotencyKey, positions.toPreferences());
    }

    @DeleteMapping("/reservations/me")
    ParticipationView cancel(
        @PathVariable long meetupId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return useCase.cancel(meetupId, memberId(jwt), idempotencyKey);
    }

    @PostMapping("/close")
    ParticipationView close(
        @PathVariable long meetupId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return useCase.close(meetupId, memberId(jwt), idempotencyKey);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "참가 준비 상태 변경")
    @org.springframework.web.bind.annotation.PutMapping("/readiness")
    org.springframework.http.ResponseEntity<Void> ready(@PathVariable long meetupId,
        @AuthenticationPrincipal Jwt jwt, @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ReadyRequest request) {
        useCase.ready(meetupId, memberId(jwt), request.ready());
        return org.springframework.http.ResponseEntity.noContent().build();
    }
    record ReadyRequest(@jakarta.validation.constraints.NotNull Boolean ready) { }

    private long memberId(Jwt jwt) {
        if (jwt == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Authentication is required.");
        }
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION_SUBJECT",
                "Authentication subject is invalid.");
        }
    }
}
