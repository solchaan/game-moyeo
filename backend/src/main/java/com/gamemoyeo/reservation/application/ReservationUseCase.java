package com.gamemoyeo.reservation.application;

import java.util.List;

public interface ReservationUseCase {

    void initializeOwner(long meetupId, long ownerId);

    ParticipationView participate(long meetupId, long memberId, String idempotencyKey);

    ParticipationView cancel(long meetupId, long memberId, String idempotencyKey);

    ParticipationView close(long meetupId, long memberId, String idempotencyKey);

    ParticipationView state(long meetupId, Long memberId);

    void closeDueMeetups();

    record ParticipationView(
        long meetupId,
        int participantCount,
        int capacity,
        int remainingCapacity,
        String recruitmentStatus,
        String participationStatus,
        String closedReason,
        boolean canParticipate,
        boolean canCancel,
        boolean canClose,
        List<String> availableActions
    ) {
    }
}
