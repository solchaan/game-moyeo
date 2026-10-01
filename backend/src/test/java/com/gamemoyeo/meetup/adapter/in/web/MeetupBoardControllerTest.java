package com.gamemoyeo.meetup.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MeetupBoardController.class, properties = "spring.mvc.problemdetails.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
class MeetupBoardControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private MeetupBoardUseCase useCase;

    @BeforeEach
    void authenticate() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pastStartReturnsFieldErrorsEvenWithBootProblemDetailsEnabled() throws Exception {
        mvc.perform(post("/api/v1/meetups").contentType(MediaType.APPLICATION_JSON)
                .content(payload("2000-01-01T00:00:00Z")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startsAt"));
        verifyNoInteractions(useCase);
    }

    @Test
    void malformedDateReturnsActionableBadRequest() throws Exception {
        mvc.perform(post("/api/v1/meetups").contentType(MediaType.APPLICATION_JSON)
                .content(payload("invalid-date")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startsAt"));
        verifyNoInteractions(useCase);
    }

    @Test
    void businessValidationIncludesItsField() throws Exception {
        when(useCase.create(eq(7L), any())).thenThrow(new ApiException(HttpStatus.BAD_REQUEST,
            "INVALID_MEETUP_OPTION", "Meetup time range is invalid.",
            Map.of("endsAt", "종료 시간은 시작 시간보다 뒤로 설정해 주세요.")));
        mvc.perform(post("/api/v1/meetups").contentType(MediaType.APPLICATION_JSON)
                .content(payload(Instant.now().plusSeconds(3600).toString())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("endsAt"));
    }

    @Test
    void validRequestCreatesMeetup() throws Exception {
        Instant start = Instant.now().plusSeconds(3600);
        when(useCase.create(eq(7L), any())).thenReturn(new MeetupBoardUseCase.MeetupView(
            99L, 1L, 7L, "Test", null, null, null, null, null, null,
            "CASUAL", "OPTIONAL", "FIRST_COME", null, start, start.plusSeconds(3600),
            5, 1, "OPEN", Map.of()));
        mvc.perform(post("/api/v1/meetups").contentType(MediaType.APPLICATION_JSON)
                .content(payload(start.toString())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(99));
    }

    @Test
    void serverQueryIsPassedToUseCase() throws Exception {
        when(useCase.findAll(1L, 10L, 200L, 2)).thenReturn(
            new MeetupBoardUseCase.CursorPage(java.util.List.of(), null, false));
        mvc.perform(get("/api/v1/meetups?gameId=1&regionOptionId=10&cursor=200&size=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void rejectsNonpositiveServerId() throws Exception {
        mvc.perform(get("/api/v1/meetups?gameId=1&regionOptionId=0"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    private String payload(String startsAt) {
        return """
            {"gameId":1,"title":"Test","playStyle":"CASUAL","voiceChatPolicy":"OPTIONAL",
             "approvalType":"FIRST_COME","startsAt":"%s","endsAt":"%s","capacity":5,
             "roleRequirements":[]}
            """.formatted(startsAt, Instant.now().plusSeconds(7200));
    }
}
