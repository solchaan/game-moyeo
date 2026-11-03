package com.gamemoyeo.member.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gamemoyeo.member.application.CommunityUseCase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommunityController.class)
@AutoConfigureMockMvc(addFilters=false)
@Import(CommunityControllerTest.MethodSecurity.class)
class CommunityControllerTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurity { }
    @Autowired MockMvc mvc;
    @MockitoBean CommunityUseCase useCase;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void authenticate(String role) {
        var jwt = Jwt.withTokenValue("test").header("alg","none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    }
    @Test void memberCannotReadOrResolveReports() throws Exception {
        authenticate("MEMBER");
        mvc.perform(get("/api/v1/admin/reports")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/reports/1/resolution").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"RESOLVED\",\"resolution\":\"unauthorized\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }
    @Test void administratorCanReviewAndRecordsAuthenticatedActor() throws Exception {
        authenticate("ADMIN");
        when(useCase.reports(null,20)).thenReturn(new CommunityUseCase.ReportPage(List.of(),null,false));
        mvc.perform(get("/api/v1/admin/reports")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/reports/1/resolution").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"DISMISSED\",\"resolution\":\"Reviewed\",\"adminId\":99}")).andExpect(status().isNoContent());
        verify(useCase).resolve(7,1,"DISMISSED","Reviewed");
    }
}
