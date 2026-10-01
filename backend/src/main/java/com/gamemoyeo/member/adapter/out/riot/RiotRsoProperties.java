package com.gamemoyeo.member.adapter.out.riot;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RiotRsoProperties {
    public final boolean enabled;
    public final String clientId;
    public final String clientSecret;
    public final URI callbackUri;
    public final URI frontendUri;
    public RiotRsoProperties(
        @Value("${app.riot.enabled:false}") boolean enabled,
        @Value("${app.riot.client-id:}") String clientId,
        @Value("${app.riot.client-secret:}") String clientSecret,
        @Value("${app.riot.callback-uri:https://gamemoyeo.noroo.kr/api/v1/riot/callback}") URI callbackUri,
        @Value("${app.riot.frontend-uri:https://gamemoyeo.noroo.kr/riot/callback}") URI frontendUri
    ) {
        if (enabled && (clientId.isBlank() || clientSecret.isBlank()
            || !"https".equals(callbackUri.getScheme()) || !"https".equals(frontendUri.getScheme())
            || callbackUri.getHost() == null || frontendUri.getHost() == null
            || callbackUri.getRawQuery() != null || frontendUri.getRawQuery() != null
            || callbackUri.getFragment() != null || frontendUri.getFragment() != null
            || callbackUri.getUserInfo() != null || frontendUri.getUserInfo() != null
            || !"/api/v1/riot/callback".equals(callbackUri.getPath())
            || !"/riot/callback".equals(frontendUri.getPath()))) {
            throw new IllegalStateException("Riot RSO requires client credentials and fixed HTTPS callback URLs");
        }
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUri = callbackUri;
        this.frontendUri = frontendUri;
    }
}
