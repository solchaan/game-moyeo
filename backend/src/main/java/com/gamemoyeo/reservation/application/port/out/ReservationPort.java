package com.gamemoyeo.reservation.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface ReservationPort {

    boolean initializeOwner(long meetupId, long ownerId);

    Optional<ReservationState> findState(long meetupId, Long memberId);

    boolean lockMeetup(long meetupId);

    void confirmReservation(long sessionId, long memberId);

    boolean claimSeat(long meetupId);

    void closeIfFull(long meetupId);

    boolean cancelReservation(long sessionId, long memberId);

    void releaseSeat(long meetupId);

    void reopenIfPossible(long meetupId);

    boolean closeByOwner(long meetupId, long ownerId);

    void closeDueMeetups();

    record ReservationState(
        long meetupId,
        long sessionId,
        long ownerId,
        String meetupStatus,
        String sessionStatus,
        String closedReason,
        Instant startsAt,
        Instant recruitmentDeadline,
        int capacity,
        int participantCount,
        String reservationStatus
    ) {
    }
}
