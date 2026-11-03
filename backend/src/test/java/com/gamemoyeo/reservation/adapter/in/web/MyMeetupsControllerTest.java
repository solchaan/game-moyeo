package com.gamemoyeo.reservation.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyMeetupsController.class)
@AutoConfigureMockMvc(addFilters = false)
class MyMeetupsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean MyMeetupsUseCase useCase;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/members/me/meetups")).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }
    @Test void usesOnlyAuthenticatedIdentity() throws Exception {
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(useCase.find(7, MyMeetupsUseCase.Filter.HOSTED, null, 20))
            .thenReturn(new MyMeetupsUseCase.Page(List.of(), null, false));
        mvc.perform(get("/api/v1/members/me/meetups?filter=HOSTED&memberId=8"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hasNext").value(false));
        verify(useCase).find(7, MyMeetupsUseCase.Filter.HOSTED, null, 20);
    }
}
