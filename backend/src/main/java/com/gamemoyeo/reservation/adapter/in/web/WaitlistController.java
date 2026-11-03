package com.gamemoyeo.reservation.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.WaitlistUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meetups/{meetupId}/waitlist/me")
public class WaitlistController {
    private final WaitlistUseCase useCase;
    public WaitlistController(WaitlistUseCase useCase) { this.useCase = useCase; }
    @GetMapping
    WaitlistUseCase.View find(@PathVariable long meetupId,@AuthenticationPrincipal Jwt jwt) { return useCase.find(meetupId,member(jwt)); }
    @PutMapping
    WaitlistUseCase.View join(@PathVariable long meetupId,@AuthenticationPrincipal Jwt jwt) { return useCase.join(meetupId,member(jwt)); }
    @DeleteMapping
    WaitlistUseCase.View cancel(@PathVariable long meetupId,@AuthenticationPrincipal Jwt jwt) { return useCase.cancel(meetupId,member(jwt)); }
    private long member(Jwt jwt) {
        if (jwt==null) { throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Authentication is required."); }
        try { return Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException error) { throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_AUTHENTICATION_SUBJECT","Invalid subject."); }
    }
}
