package com.gamemoyeo.member.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.gamemoyeo.common.exception.GlobalExceptionHandler;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GameProfileControllerTest {
    private final GameProfileUseCase service = mock(GameProfileUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new GameProfileController(service))
        .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }
    private void login() {
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
    @Test
    void anonymousReadAndWriteAreUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/members/me/games/1/profile")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/members/me/games/1/profile").contentType("application/json")
            .content("{\"primaryPosition\":\"MID\"}" )).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/members/me/games/1/profile/nickname").contentType("application/json")
            .content("{\"nickname\":\"user#KR1\"}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test
    void usesAuthenticatedMemberAndReturnsSavedPositions() throws Exception {
        login();
        var positions = new PositionPreferences(Position.MID, Position.SUPPORT);
        when(service.save(7, 1, positions)).thenReturn(positions);
        mvc.perform(put("/api/v1/members/me/games/1/profile").contentType("application/json")
            .content("""
                {"memberId":99,"primaryPosition":"MID","secondaryPosition":"SUPPORT"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.primaryPosition").value("MID"));
        verify(service).save(7, 1, positions);
    }
    @Test
    void savesNicknameForAuthenticatedMember() throws Exception {
        login();
        when(service.saveNickname(7, 1, "user#KR1")).thenReturn("user#KR1");
        mvc.perform(put("/api/v1/members/me/games/1/profile/nickname").contentType("application/json")
            .content("{\"memberId\":99,\"nickname\":\"user#KR1\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("user#KR1"));
        verify(service).saveNickname(7, 1, "user#KR1");
    }
    @Test
    void rejectsDuplicateUnknownAndSecondaryOnlyPositions() throws Exception {
        login();
        for (String body : java.util.List.of(
            "{\"primaryPosition\":\"MID\",\"secondaryPosition\":\"MID\"}",
            "{\"primaryPosition\":\"INVALID\"}",
            "{\"secondaryPosition\":\"MID\"}")) {
            mvc.perform(put("/api/v1/members/me/games/1/profile").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
