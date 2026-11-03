package com.gamemoyeo.reservation.adapter.in.scheduling;

import com.gamemoyeo.reservation.application.ReservationUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="spring.task.scheduling.enabled", havingValue="true", matchIfMissing=true)
public class ReservationCloseScheduler {

    private final ReservationUseCase useCase;

    public ReservationCloseScheduler(ReservationUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${app.reservation.close-check-delay:15000}")
    public void closeDueMeetups() {
        useCase.closeDueMeetups();
        useCase.remindUpcoming();
    }
}
