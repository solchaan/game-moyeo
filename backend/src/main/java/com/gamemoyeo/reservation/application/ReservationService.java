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
    private final com.gamemoyeo.member.application.CommunityUseCase community;
    private final com.gamemoyeo.reservation.application.port.out.WaitlistPort waitlist;
    private final com.gamemoyeo.notification.application.NotificationUseCase notifications;
    private final com.gamemoyeo.reservation.application.port.out.ReservationIdempotencyPort idempotency;

    private final ParticipationPreferencesService preferences;

    @Autowired
    public ReservationService(ReservationPort port, ParticipationPreferencesService preferences,
        com.gamemoyeo.reservation.application.port.out.ReservationIdempotencyPort idempotency, com.gamemoyeo.notification.application.NotificationUseCase notifications, com.gamemoyeo.reservation.application.port.out.WaitlistPort waitlist, com.gamemoyeo.member.application.CommunityUseCase community) {
        this(port, preferences, idempotency, notifications, waitlist, community, Clock.systemUTC());
    }

    ReservationService(ReservationPort port, ParticipationPreferencesService preferences,
        com.gamemoyeo.reservation.application.port.out.ReservationIdempotencyPort idempotency, com.gamemoyeo.notification.application.NotificationUseCase notifications, com.gamemoyeo.reservation.application.port.out.WaitlistPort waitlist, com.gamemoyeo.member.application.CommunityUseCase community, Clock clock) {
        this.port = port;
        this.preferences = preferences;
        this.clock = clock;
        this.idempotency = idempotency;
        this.notifications = notifications;
        this.waitlist = waitlist;
        this.community = community;
    }

    @Override
    @Transactional
    public void initializeOwner(long meetupId, long ownerId) {
        if (!port.initializeOwner(meetupId, ownerId)) {
            throw notFound();
        }
        var positions = preferences.resolve(meetupId, ownerId, null);
        port.savePositions(requireState(meetupId, ownerId).sessionId(), ownerId, positions);
    }

    @Override
    @Transactional
    public ParticipationView participate(long meetupId, long memberId, String idempotencyKey) {
        return participate(meetupId, memberId, idempotencyKey, null);
    }

    @Override
    @Transactional
    public ParticipationView participate(long meetupId, long memberId, String idempotencyKey,
        com.gamemoyeo.member.domain.PositionPreferences positions) {
        validateIdempotencyKey(idempotencyKey);
        if (!port.lockMeetup(meetupId)) { throw notFound(); }
        String operation = "PARTICIPATE:" + meetupId;
        String payload = positions == null ? "DEFAULT" : String.valueOf(positions.primaryPosition()) + ":" + positions.secondaryPosition();
        var replay = idempotency.replay(memberId, operation, idempotencyKey, payload);
        if (replay.isPresent()) { return replay.get(); }
        ReservationState before = requireState(meetupId, memberId);
        if (before.ownerId() == memberId || "CONFIRMED".equals(before.reservationStatus())) {
            return remember(memberId, operation, idempotencyKey, payload, view(before, memberId));
        }
        ensureOpen(before);
        var selected = preferences.resolve(meetupId, memberId, positions);

        community.guardAdmission(meetupId, memberId);
        waitlist.guardAdmission(meetupId, memberId);
        if (!port.claimSeat(meetupId)) {
            ensureOpen(requireState(meetupId, memberId));
            throw new ApiException(HttpStatus.CONFLICT, "RESERVATION_CAPACITY_EXCEEDED",
                "The meetup has no remaining capacity.");
        }
        port.confirmReservation(before.sessionId(), memberId);
        port.savePositions(before.sessionId(), memberId, selected);
        port.closeIfFull(meetupId);
        waitlist.accepted(meetupId, memberId);
        notifications.publish(List.of(before.ownerId(), memberId), meetupId, "PARTICIPATION", "모임 참가가 확정되었습니다.", null);
        return remember(memberId, operation, idempotencyKey, payload, view(requireState(meetupId, memberId), memberId));
    }

    @Override
    @Transactional
    public ParticipationView cancel(long meetupId, long memberId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        if (!port.lockMeetup(meetupId)) { throw notFound(); }
        String operation = "CANCEL:" + meetupId;
        String payload = "";
        var replay = idempotency.replay(memberId, operation, idempotencyKey, payload);
        if (replay.isPresent()) { return replay.get(); }
        ReservationState before = requireState(meetupId, memberId);
        if (before.ownerId() == memberId) {
            throw new ApiException(HttpStatus.CONFLICT, "MEETUP_OWNER_CANNOT_CANCEL",
                "The meetup owner cannot cancel their participation.");
        }
        if (!"CONFIRMED".equals(before.reservationStatus())) {
            return remember(memberId, operation, idempotencyKey, payload, view(before, memberId));
        }
        if (port.cancelReservation(before.sessionId(), memberId)) {
            port.releaseSeat(meetupId);
            port.reopenIfPossible(meetupId);
            waitlist.advance(meetupId);
            notifications.publish(List.of(before.ownerId(), memberId), meetupId, "CANCELLATION", "모임 참가가 취소되었습니다.", null);
        }
        return remember(memberId, operation, idempotencyKey, payload, view(requireState(meetupId, memberId), memberId));
    }

    @Override
    @Transactional
    public ParticipationView close(long meetupId, long memberId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        if (!port.lockMeetup(meetupId)) { throw notFound(); }
        String operation = "CLOSE:" + meetupId;
        String payload = "";
        var replay = idempotency.replay(memberId, operation, idempotencyKey, payload);
        if (replay.isPresent()) { return replay.get(); }
        ReservationState before = requireState(meetupId, memberId);
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
        return remember(memberId, operation, idempotencyKey, payload, view(requireState(meetupId, memberId), memberId));
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

    @Override
    @Transactional
    public void notifyChange(long meetupId, String kind, String message) {
        notifications.publish(port.confirmedMembers(meetupId), meetupId, kind, message, null);
    }

    @Override
    @Transactional
    public void remindUpcoming() {
        for (var reminder : port.reminders()) {
            notifications.publish(port.confirmedMembers(reminder.meetupId()), reminder.meetupId(), "REMINDER",
                "모임 시작이 30분 이내로 다가왔습니다. 일정을 확인해 주세요.", reminder.startsAt().toString());
        }
    }

    @Override
    @Transactional
    public void ready(long meetupId, long memberId, boolean ready) {
        var state = requireLockedState(meetupId, memberId);
        if (!"CONFIRMED".equals(state.reservationStatus()) || !state.startsAt().isAfter(clock.instant())) {
            throw new ApiException(HttpStatus.CONFLICT, "READINESS_NOT_AVAILABLE", "Only upcoming participants can update readiness.");
        }
        port.ready(state.sessionId(), memberId, ready);
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
        if (key == null || key.isBlank() || key.length() > 100 || !key.equals(key.strip())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                "Idempotency-Key must contain between 1 and 100 characters.");
        }
    }

    private ParticipationView remember(long memberId, String operation, String key, String payload, ParticipationView result) {
        idempotency.save(memberId, operation, key, payload, result);
        return result;
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "MEETUP_NOT_FOUND", "Meetup was not found.");
    }
}
