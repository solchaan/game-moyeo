package com.gamemoyeo.reservation.application;

import com.gamemoyeo.member.application.port.in.PositionSelection;
import java.util.List;

public interface PositionAvailabilityUseCase {
    PositionAvailabilityView find(long meetupId);

    record PositionAvailabilityView(boolean enabled, boolean requirementsConfigured, int remainingCapacity,
        int unassignedCount, int anyPositionCount, PositionSelection ownerPositions, List<PositionSlot> positions) { }

    record PositionSlot(String position, int participantCount, Integer capacity, Integer remainingCapacity,
        String status, boolean includesOwner, List<String> nicknames) { }
}
