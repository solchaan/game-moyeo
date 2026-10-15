package com.gamemoyeo.reservation.adapter.in.web;

import com.gamemoyeo.reservation.application.PositionAvailabilityUseCase;
import com.gamemoyeo.reservation.application.PositionAvailabilityUseCase.PositionAvailabilityView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PositionAvailabilityController {
    private final PositionAvailabilityUseCase positions;

    public PositionAvailabilityController(PositionAvailabilityUseCase positions) { this.positions = positions; }

    @GetMapping("/api/v1/meetups/{meetupId}/positions")
    public PositionAvailabilityView find(@PathVariable long meetupId) { return positions.find(meetupId); }
}
