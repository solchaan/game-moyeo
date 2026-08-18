package com.gamemoyeo.auth.adapter.in.web;

import com.gamemoyeo.auth.application.MemberAuthenticationService;
import com.gamemoyeo.auth.application.TokenService;
import com.gamemoyeo.auth.application.TokenService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class MemberAuthController {

    private final MemberAuthenticationService authenticationService;
    private final TokenService tokenService;

    public MemberAuthController(MemberAuthenticationService authenticationService, TokenService tokenService) {
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    ResponseEntity<TokenPair> register(@Valid @RequestBody RegisterRequest request) {
        long memberId = authenticationService.register(
            request.username(), request.password(), request.nickname(), request.email());
        return noStore(ResponseEntity.created(URI.create("/api/v1/members/" + memberId)))
            .body(tokenService.issue(memberId));
    }

    @PostMapping("/login")
    ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        long memberId = authenticationService.authenticate(request.username(), request.password());
        return noStore(ResponseEntity.ok()).body(tokenService.issue(memberId));
    }

    private ResponseEntity.BodyBuilder noStore(ResponseEntity.BodyBuilder response) {
        return response.cacheControl(CacheControl.noStore()).header("Pragma", "no-cache");
    }

    record LoginRequest(
        @NotBlank @Size(max = 40) String username,
        @NotBlank @Size(max = 72) String password
    ) {
    }

    record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 40)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "must contain only letters, numbers, or underscores")
        String username,
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$",
            message = "must contain at least one letter and one number")
        String password,
        @NotBlank @Size(min = 2, max = 40) String nickname,
        @Email @Size(max = 254) String email
    ) {
    }
}
