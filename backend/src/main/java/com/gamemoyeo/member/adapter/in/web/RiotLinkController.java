package com.gamemoyeo.member.adapter.in.web;

import com.gamemoyeo.member.application.port.in.RiotLinkUseCase;
import com.gamemoyeo.member.adapter.out.riot.RiotRsoProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Riot account linking")
@RestController
public class RiotLinkController {
    private static final String COOKIE = "riot_link_browser";
    private static final String CALLBACK = "/api/v1/riot/callback";
    private final RiotLinkUseCase service;
    private final RiotRsoProperties properties;
    public RiotLinkController(RiotLinkUseCase service, RiotRsoProperties properties) {
        this.service = service;
        this.properties = properties;
    }
    @GetMapping("/api/v1/members/me/riot")
    public ResponseEntity<RiotLinkUseCase.Status> status(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(memberId(jwt)));
    }
    @PostMapping("/api/v1/members/me/riot/authorization")
    public ResponseEntity<AuthorizationResponse> start(@AuthenticationPrincipal Jwt jwt) {
        var start = service.start(memberId(jwt));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.SET_COOKIE, cookie(start.browser(), Duration.ofMinutes(5)).toString())
            .body(new AuthorizationResponse(start.authorizationUrl()));
    }
    @GetMapping(CALLBACK)
    public void callback(@RequestParam(required = false) String state,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String error,
        @CookieValue(name = COOKIE, required = false) String browser,
        HttpServletResponse response) throws java.io.IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        String target;
        try {
            // A provider cancellation consumes valid state as well, preventing reuse.
            String ticket = service.callback(state, browser, error == null ? code : null);
            target = UriComponentsBuilder.fromUri(properties.frontendUri).queryParam("code", ticket).build().toUriString();
        } catch (RuntimeException exception) {
            // Do not put provider responses, credentials, codes, or exception details in redirects/logs.
            target = UriComponentsBuilder.fromUri(properties.frontendUri).queryParam("error", "failed").build().toUriString();
        }
        response.sendRedirect(target);
    }
    @PostMapping("/api/v1/members/me/riot")
    public ResponseEntity<Void> complete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CompleteRequest body) {
        service.complete(memberId(jwt), body.code());
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
    @DeleteMapping("/api/v1/members/me/riot")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal Jwt jwt) {
        service.unlink(memberId(jwt));
        return ResponseEntity.noContent().build();
    }
    private long memberId(Jwt jwt) {
        if (jwt == null) {
            throw new com.gamemoyeo.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
        }
        return Long.parseLong(jwt.getSubject());
    }
    private ResponseCookie cookie(String value, Duration ttl) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(true).sameSite("Lax")
            .path(CALLBACK).maxAge(ttl).build();
    }
    public record AuthorizationResponse(String authorizationUrl) { }
    public record CompleteRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String code) { }
}
