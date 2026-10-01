package com.gamemoyeo.member.adapter.out.riot;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.out.RiotAuthorizationPort;
import com.gamemoyeo.member.domain.RiotAccount;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RiotAuthorizationAdapter implements RiotAuthorizationPort {
    private final RiotRsoProperties properties;
    private final RestClient client;
    @org.springframework.beans.factory.annotation.Autowired
    public RiotAuthorizationAdapter(RiotRsoProperties properties) {
        this.properties = properties;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(factory).build();
    }
    RiotAuthorizationAdapter(RiotRsoProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }
    @Override
    public boolean enabled() {
        return properties.enabled;
    }
    @Override
    public String authorizationUrl(String state, String challenge) {
        return UriComponentsBuilder.fromUriString("https://auth.riotgames.com/authorize")
            .queryParam("client_id", properties.clientId)
            .queryParam("redirect_uri", properties.callbackUri.toString())
            .queryParam("response_type", "code").queryParam("scope", "openid")
            .queryParam("state", state).queryParam("code_challenge", challenge)
            .queryParam("code_challenge_method", "S256").build().encode().toUriString();
    }
    @Override
    public RiotAccount exchange(String code, String verifier) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.callbackUri.toString());
        form.add("code_verifier", verifier);
        try {
            TokenResponse token = client.post().uri("https://auth.riotgames.com/token")
                .headers(h -> h.setBasicAuth(properties.clientId, properties.clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TokenResponse.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()
                || !"Bearer".equalsIgnoreCase(token.tokenType())) {
                throw unavailable();
            }
            // Authenticate ownership using Riot's protected /me endpoint, never a user-supplied Riot ID.
            // Provider access/refresh tokens and ID tokens are neither persisted nor returned to the browser.
            RiotAccount account = client.get().uri("https://asia.api.riotgames.com/riot/account/v1/accounts/me")
                .headers(h -> h.setBearerAuth(token.accessToken())).retrieve().body(RiotAccount.class);
            if (account == null) {
                throw unavailable();
            }
            return account;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw unavailable();
        }
    }
    private ApiException unavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "RIOT_AUTH_FAILED", "Riot 인증을 완료하지 못했습니다. 다시 시도해 주세요.");
    }
    record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("token_type") String tokenType) { }
}
