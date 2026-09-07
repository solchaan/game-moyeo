package com.gamemoyeo.reservation.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.port.out.ReservationPort;
import com.gamemoyeo.reservation.application.port.out.ReservationPort.ReservationState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService implements ReservationUseCase {

    private static final Duration PARTICIPATION_CUTOFF = Duration.ofMinutes(10);

    private final ReservationPort port;
    private final Clock clock;

    @Autowired
    public ReservationService(ReservationPort port) {
        this(port, Clock.systemUTC());
    }

    ReservationService(ReservationPort port, Clock clock) {
        this.port = port;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void initializeOwner(long meetupId, long ownerId) {
        if (!port.initializeOwner(meetupId, ownerId)) {
            throw notFound();
        }
    }

    @Override
    @Transactional
    public ParticipationView participate(long meetupId, long memberId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        ReservationState before = requireLockedState(meetupId, memberId);
        if (before.ownerId() == memberId || "CONFIRMED".equals(before.reservationStatus())) {
            return view(before, memberId);
        }
        ensureOpen(before);

        if (!port.claimSeat(meetupId)) {
            ensureOpen(requireState(meetupId, memberId));
            throw new ApiException(HttpStatus.CONFLICT, "RESERVATION_CAPACITY_EXCEEDED",
                "The meetup has no remaining capacity.");
        }
        port.confirmReservation(before.sessionId(), memberId);
        port.closeIfFull(meetupId);
        return view(requireState(meetupId, memberId), memberId);
    }

    @Override
    @Transactional
    public ParticipationView cancel(long meetupId, long memberId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        ReservationState before = requireLockedState(meetupId, memberId);
        if (before.ownerId() == memberId) {
            throw new ApiException(HttpStatus.CONFLICT, "MEETUP_OWNER_CANNOT_CANCEL",
                "The meetup owner cannot cancel their participation.");
        }
        if (!"CONFIRMED".equals(before.reservationStatus())) {
            return view(before, memberId);
        }
        if (port.cancelReservation(before.sessionId(), memberId)) {
            port.releaseSeat(meetupId);
            port.reopenIfPossible(meetupId);
        }
        return view(requireState(meetupId, memberId), memberId);
    }

    @Override
    @Transactional
    public ParticipationView close(long meetupId, long memberId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        ReservationState before = requireLockedState(meetupId, memberId);
        if (before.ownerId() != memberId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEETUP_OWNER_REQUIRED",
                "Only the meetup owner can close participation.");
        }
        if ("DELETED".equals(before.meetupStatus())) {
            throw notFound();
        }
        if (isOpen(before)) {
            port.closeByOwner(meetupId, memberId);
        }
        return view(requireState(meetupId, memberId), memberId);
    }

    @Override
    public ParticipationView state(long meetupId, Long memberId) {
        return view(requireState(meetupId, memberId), memberId);
    }

    @Override
    @Transactional
    public void closeDueMeetups() {
        port.closeDueMeetups();
    }

    private ParticipationView view(ReservationState state, Long memberId) {
        boolean open = isOpen(state);
        boolean owner = memberId != null && state.ownerId() == memberId;
        boolean joined = "CONFIRMED".equals(state.reservationStatus());
        String participationStatus = owner ? "OWNER" : joined ? "JOINED" : "NOT_JOINED";
        boolean canParticipate = memberId != null && open && !joined;
        boolean canCancel = memberId != null && joined && !owner;
        boolean canClose = owner && open;
        List<String> actions = new ArrayList<>();
        if (canParticipate) {
            actions.add("PARTICIPATE");
        }
        if (canCancel) {
            actions.add("CANCEL");
        }
        if (canClose) {
            actions.add("CLOSE");
        }
        return new ParticipationView(state.meetupId(), state.participantCount(), state.capacity(),
            Math.max(0, state.capacity() - state.participantCount()), open ? "OPEN" : "CLOSED",
            participationStatus, effectiveClosedReason(state), canParticipate, canCancel, canClose,
            List.copyOf(actions));
    }

    private void ensureOpen(ReservationState state) {
        if (state.participantCount() >= state.capacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "RESERVATION_CAPACITY_EXCEEDED",
                "The meetup has no remaining capacity.");
        }
        if (!isOpen(state)) {
            throw new ApiException(HttpStatus.CONFLICT, "MEETUP_PARTICIPATION_CLOSED",
                "Participation for this meetup is closed.");
        }
    }

    private boolean isOpen(ReservationState state) {
        Instant now = clock.instant();
        return "OPEN".equals(state.meetupStatus())
            && "OPEN".equals(state.sessionStatus())
            && state.participantCount() < state.capacity()
            && state.startsAt().isAfter(now.plus(PARTICIPATION_CUTOFF))
            && (state.recruitmentDeadline() == null || state.recruitmentDeadline().isAfter(now));
    }

    private String effectiveClosedReason(ReservationState state) {
        if (isOpen(state)) {
            return null;
        }
        if (state.closedReason() != null) {
            return state.closedReason();
        }
        if (state.participantCount() >= state.capacity()) {
            return "FULL";
        }
        Instant now = clock.instant();
        if (!state.startsAt().isAfter(now.plus(PARTICIPATION_CUTOFF))) {
            return "START_IMMINENT";
        }
        if (state.recruitmentDeadline() != null && !state.recruitmentDeadline().isAfter(now)) {
            return "DEADLINE_REACHED";
        }
        return "CLOSED";
    }

    private ReservationState requireLockedState(long meetupId, long memberId) {
        if (!port.lockMeetup(meetupId)) {
            throw notFound();
        }
        return requireState(meetupId, memberId);
    }

    private ReservationState requireState(long meetupId, Long memberId) {
        ReservationState state = port.findState(meetupId, memberId).orElseThrow(this::notFound);
        if ("DELETED".equals(state.meetupStatus())) {
            throw notFound();
        }
        return state;
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                "Idempotency-Key must contain between 1 and 100 characters.");
        }
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "MEETUP_NOT_FOUND", "Meetup was not found.");
    }
}
