package com.gamemoyeo.reservation.adapter.in.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.common.exception.GlobalExceptionHandler;
import com.gamemoyeo.member.application.port.in.PositionSelection;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import com.gamemoyeo.reservation.application.PositionAvailabilityUseCase;
import com.gamemoyeo.reservation.application.PositionAvailabilityUseCase.PositionAvailabilityView;
import com.gamemoyeo.reservation.application.PositionAvailabilityUseCase.PositionSlot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PositionAvailabilityControllerTest {
    @Test
    void publiclyShowsCountsAndOwnerPositionWithoutExposingMemberIds() throws Exception {
        var service = mock(PositionAvailabilityUseCase.class);
        when(service.find(1)).thenReturn(new PositionAvailabilityView(true, true, 4, 0, 0,
            new PositionSelection(Position.TOP, null), List.of(new PositionSlot("TOP", 1, 2, 1, "OPEN", true, List.of("탑유저#KR1")))));
        var mvc = MockMvcBuilders.standaloneSetup(new PositionAvailabilityController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/meetups/1/positions")).andExpect(status().isOk())
            .andExpect(jsonPath("$.ownerPositions.primaryPosition").value("TOP"))
            .andExpect(jsonPath("$.positions[0].remainingCapacity").value(1))
            .andExpect(jsonPath("$.positions[0].includesOwner").value(true))
            .andExpect(jsonPath("$.positions[0].memberId").doesNotExist());
        when(service.find(2)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "MEETUP_NOT_FOUND", "Not found"));
        mvc.perform(get("/api/v1/meetups/2/positions")).andExpect(status().isNotFound());
    }
}
