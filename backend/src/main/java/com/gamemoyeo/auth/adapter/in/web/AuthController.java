package com.gamemoyeo.auth.adapter.in.web;

import com.gamemoyeo.auth.application.LoginCodeService;
import com.gamemoyeo.auth.application.TokenService;
import com.gamemoyeo.auth.application.TokenService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginCodeService loginCodeService;
    private final TokenService tokenService;

    public AuthController(LoginCodeService loginCodeService, TokenService tokenService) {
        this.loginCodeService = loginCodeService;
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    ResponseEntity<TokenPair> exchange(@Valid @RequestBody LoginCodeRequest request) {
        long memberId = loginCodeService.consume(request.code());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("Pragma", "no-cache")
            .body(tokenService.issue(memberId));
    }

    public record LoginCodeRequest(@NotBlank String code) {
    }
}
