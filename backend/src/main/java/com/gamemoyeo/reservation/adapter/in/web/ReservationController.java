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

    public ReservationController(ReservationUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/participation")
    ParticipationView state(@PathVariable long meetupId, @AuthenticationPrincipal Jwt jwt) {
        return useCase.state(meetupId, jwt == null ? null : memberId(jwt));
    }

    @PostMapping("/reservations")
    ParticipationView participate(
        @PathVariable long meetupId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return useCase.participate(meetupId, memberId(jwt), idempotencyKey);
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
