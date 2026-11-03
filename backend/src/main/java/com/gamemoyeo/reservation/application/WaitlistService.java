package com.gamemoyeo.reservation.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.port.out.ReservationPort;
import com.gamemoyeo.reservation.application.port.out.WaitlistPort;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WaitlistService implements WaitlistUseCase {
    private final WaitlistPort port;
    private final ReservationPort reservations;
    public WaitlistService(WaitlistPort port, ReservationPort reservations) {
        this.port = port;
        this.reservations = reservations;
    }
    @Override
    @Transactional(readOnly=true)
    public View find(long meetupId, long memberId) {
        var state = state(meetupId,memberId);
        boolean eligible = state.ownerId()!=memberId && !"CONFIRMED".equals(state.reservationStatus())
            && state.participantCount()>=state.capacity() && "FULL".equals(state.closedReason())
            && state.startsAt().isAfter(Instant.now().plusSeconds(600))
            && (state.recruitmentDeadline()==null || state.recruitmentDeadline().isAfter(Instant.now()));
        var view = port.find(meetupId,memberId,eligible);
        boolean open = "OPEN".equals(state.meetupStatus()) && "OPEN".equals(state.sessionStatus())
            && state.participantCount()<state.capacity() && state.startsAt().isAfter(Instant.now().plusSeconds(600))
            && (state.recruitmentDeadline()==null || state.recruitmentDeadline().isAfter(Instant.now()));
        return new View(view.status(),view.position(),view.offeredUntil(),view.canJoin(),view.canAccept()&&open,view.canCancel());
    }
    @Override
    public View join(long meetupId, long memberId) {
        lock(meetupId);
        var current = find(meetupId,memberId);
        if (current.canCancel()) { return current; }
        if (!current.canJoin()) { throw new ApiException(HttpStatus.CONFLICT,"WAITLIST_NOT_AVAILABLE","Only full upcoming meetups accept waiting requests."); }
        port.join(meetupId,memberId);
        return find(meetupId,memberId);
    }
    @Override
    public View cancel(long meetupId, long memberId) {
        lock(meetupId);
        state(meetupId,memberId);
        port.cancel(meetupId,memberId);
        port.advance(meetupId);
        return find(meetupId,memberId);
    }
    @Override
    public void advance() {
        for (long meetup : port.pendingMeetups()) {
            lock(meetup);
            port.advance(meetup);
        }
    }
    private void lock(long meetup) {
        if (!reservations.lockMeetup(meetup)) { throw missing(); }
    }
    private ReservationPort.ReservationState state(long meetup, long member) {
        var state = reservations.findState(meetup,member).orElseThrow(this::missing);
        if ("DELETED".equals(state.meetupStatus())) { throw missing(); }
        return state;
    }
    private ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"MEETUP_NOT_FOUND","Meetup was not found."); }
}
