package com.gamemoyeo.member.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.gamemoyeo.member.application.port.in.RiotLinkUseCase;
import com.gamemoyeo.member.adapter.out.riot.RiotRsoProperties;
import com.gamemoyeo.common.exception.GlobalExceptionHandler;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RiotLinkControllerTest {
    @Test
    void callbackFailureIsSanitizedAndUnauthenticatedWritesAreRejected() throws Exception {
        var service = mock(RiotLinkUseCase.class);
        var properties = new RiotRsoProperties(false, "", "", URI.create("https://example.com/api/v1/riot/callback"),
            URI.create("https://example.com/riot/callback"));
        when(service.callback(any(), any(), any())).thenThrow(new IllegalStateException("sensitive-provider-response"));
        var mvc = MockMvcBuilders.standaloneSetup(new RiotLinkController(service, properties))
            .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/riot/callback").param("error", "access_denied"))
            .andExpect(status().isFound()).andExpect(header().string("Location", "https://example.com/riot/callback?error=failed"))
            .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/api/v1/members/me/riot/authorization")).andExpect(status().isUnauthorized());
    }
}
