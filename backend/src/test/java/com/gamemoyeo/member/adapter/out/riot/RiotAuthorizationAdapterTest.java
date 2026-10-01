package com.gamemoyeo.member.adapter.out.riot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import com.gamemoyeo.common.exception.ApiException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RiotAuthorizationAdapterTest {
    private final RiotRsoProperties properties = new RiotRsoProperties(true, "test-client", "test-secret",
        URI.create("https://example.com/api/v1/riot/callback"), URI.create("https://example.com/riot/callback"));
    @Test
    void exchangesAuthorizationCodeAndOnlyTrustsAuthenticatedMeResponse() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://auth.riotgames.com/token"))
            .andExpect(header("Authorization", "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ="))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("code_verifier=verifier")))
            .andRespond(withSuccess("""
                {"access_token":"provider-token","token_type":"Bearer"}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://asia.api.riotgames.com/riot/account/v1/accounts/me"))
            .andExpect(header("Authorization", "Bearer provider-token"))
            .andRespond(withSuccess("""
                {"puuid":"verified-id","gameName":"Player","tagLine":"KR1"}
                """, MediaType.APPLICATION_JSON));
        var adapter = new RiotAuthorizationAdapter(properties, builder.build());
        assertThat(adapter.exchange("code", "verifier").puuid()).isEqualTo("verified-id");
        assertThat(adapter.authorizationUrl("state", "challenge")).contains("code_challenge_method=S256", "scope=openid")
            .doesNotContain("test-secret");
        server.verify();
    }
    @Test
    void providerFailureNeverLeaksResponseBody() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://auth.riotgames.com/token"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("secret-provider-debug"));
        assertThatThrownBy(() -> new RiotAuthorizationAdapter(properties, builder.build()).exchange("code", "verifier"))
            .isInstanceOf(ApiException.class).hasMessageNotContaining("secret-provider-debug");
    }
}
