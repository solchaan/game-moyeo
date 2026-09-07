package com.gamemoyeo.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.port.out.ReservationPort;
import com.gamemoyeo.reservation.application.port.out.ReservationPort.ReservationState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-04T00:00:00Z");

    private ReservationPort port;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        port = mock(ReservationPort.class);
        when(port.lockMeetup(10L)).thenReturn(true);
        service = new ReservationService(port, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void participantClaimsSeatAndFullMeetupCloses() {
        when(port.findState(10L, 2L))
            .thenReturn(Optional.of(state(1, 2, null)))
            .thenReturn(Optional.of(state(2, 2, "CONFIRMED", "CLOSED", "FULL")));
        when(port.claimSeat(10L)).thenReturn(true);

        var result = service.participate(10L, 2L, "request-key");

        verify(port).confirmReservation(100L, 2L);
        verify(port).closeIfFull(10L);
        assertThat(result.participantCount()).isEqualTo(2);
        assertThat(result.recruitmentStatus()).isEqualTo("CLOSED");
        assertThat(result.participationStatus()).isEqualTo("JOINED");
    }

    @Test
    void closesParticipationTenMinutesBeforeStart() {
        ReservationState state = new ReservationState(10L, 100L, 1L, "OPEN", "OPEN", null,
            NOW.plusSeconds(600), null, 5, 1, null);
        when(port.findState(10L, 2L)).thenReturn(Optional.of(state));

        assertThatThrownBy(() -> service.participate(10L, 2L, "request-key"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("closed");
    }

    @Test
    void ownerCannotCancelInitialParticipation() {
        when(port.findState(10L, 1L)).thenReturn(Optional.of(state(1, 5, "CONFIRMED")));

        assertThatThrownBy(() -> service.cancel(10L, 1L, "request-key"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("owner");
    }

    @Test
    void cancellingParticipantReleasesSeat() {
        when(port.findState(10L, 2L))
            .thenReturn(Optional.of(state(2, 5, "CONFIRMED")))
            .thenReturn(Optional.of(state(1, 5, "CANCELLED")));
        when(port.cancelReservation(100L, 2L)).thenReturn(true);

        var result = service.cancel(10L, 2L, "request-key");

        verify(port).releaseSeat(10L);
        verify(port).reopenIfPossible(10L);
        assertThat(result.participationStatus()).isEqualTo("NOT_JOINED");
    }

    @Test
    void repeatedParticipationDoesNotClaimAnotherSeat() {
        when(port.findState(10L, 2L)).thenReturn(Optional.of(state(2, 5, "CONFIRMED")));

        var result = service.participate(10L, 2L, "same-request-key");

        verify(port, never()).claimSeat(10L);
        verify(port, never()).confirmReservation(100L, 2L);
        assertThat(result.participantCount()).isEqualTo(2);
        assertThat(result.participationStatus()).isEqualTo("JOINED");
    }

    private ReservationState state(int count, int capacity, String reservationStatus) {
        return state(count, capacity, reservationStatus, "OPEN", null);
    }

    private ReservationState state(
        int count,
        int capacity,
        String reservationStatus,
        String sessionStatus,
        String closedReason
    ) {
        return new ReservationState(10L, 100L, 1L, "OPEN", sessionStatus, closedReason,
            NOW.plusSeconds(3600), null, capacity, count, reservationStatus);
    }
}
