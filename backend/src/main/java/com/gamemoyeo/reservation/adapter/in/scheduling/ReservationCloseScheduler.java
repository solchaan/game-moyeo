package com.gamemoyeo.reservation.adapter.in.scheduling;

import com.gamemoyeo.reservation.application.ReservationUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCloseScheduler {

    private final ReservationUseCase useCase;

    public ReservationCloseScheduler(ReservationUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${app.reservation.close-check-delay:15000}")
    public void closeDueMeetups() {
        useCase.closeDueMeetups();
    }
}
