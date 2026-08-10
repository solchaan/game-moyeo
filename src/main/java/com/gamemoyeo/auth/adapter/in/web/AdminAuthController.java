package com.gamemoyeo.auth.adapter.in.web;

import com.gamemoyeo.auth.application.AdminAuthenticationService;
import com.gamemoyeo.auth.application.TokenService;
import com.gamemoyeo.auth.application.TokenService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin")
public class AdminAuthController {

    private final AdminAuthenticationService authenticationService;
    private final TokenService tokenService;

    public AdminAuthController(AdminAuthenticationService authenticationService, TokenService tokenService) {
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    ResponseEntity<TokenPair> login(@Valid @RequestBody AdminLoginRequest request) {
        long memberId = authenticationService.authenticate(request.username(), request.password());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("Pragma", "no-cache")
            .body(tokenService.issue(memberId));
    }

    record AdminLoginRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(max = 200) String password
    ) {
    }
}
