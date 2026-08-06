package com.gamemoyeo.auth.adapter.in.web;

import com.gamemoyeo.auth.application.LoginCodeService;
import com.gamemoyeo.member.application.SocialLoginService;
import com.gamemoyeo.member.application.SocialProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final LoginCodeService loginCodeService;
    private final URI successRedirectUri;

    public SocialLoginSuccessHandler(
        SocialLoginService socialLoginService,
        LoginCodeService loginCodeService,
        @Value("${app.social-login.success-redirect-uri}") URI successRedirectUri
    ) {
        this.socialLoginService = socialLoginService;
        this.loginCodeService = loginCodeService;
        this.successRedirectUri = successRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        SocialProfile profile = profile(token.getAuthorizedClientRegistrationId(), token.getPrincipal());
        long memberId = socialLoginService.login(profile).memberId();
        String code = loginCodeService.issue(memberId);
        String redirect = UriComponentsBuilder.fromUri(successRedirectUri)
            .queryParam("code", code)
            .build(true)
            .toUriString();
        response.sendRedirect(redirect);
    }

    @SuppressWarnings("unchecked")
    private SocialProfile profile(String provider, OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        if ("naver".equals(provider)) {
            attributes = (Map<String, Object>) attributes.get("response");
        }
        String subject = value(attributes, "sub", value(attributes, "id", null));
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("OAuth provider did not return a stable subject");
        }
        String name = value(attributes, "nickname", value(attributes, "name", provider + " user"));
        String image = value(attributes, "picture", value(attributes, "profile_image", null));
        String email = value(attributes, "email", null);
        boolean verified = Boolean.parseBoolean(value(attributes, "email_verified", "false"));
        return new SocialProfile(provider.toUpperCase(), subject, email, verified, name, image);
    }

    private String value(Map<String, Object> attributes, String key, String fallback) {
        Object value = attributes.get(key);
        return value == null ? fallback : value.toString();
    }
}
