package com.gamemoyeo.reservation.adapter.in.scheduling;

import com.gamemoyeo.reservation.application.WaitlistUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="spring.task.scheduling.enabled", havingValue="true", matchIfMissing=true)
public class WaitlistScheduler {
    private final WaitlistUseCase useCase;
    public WaitlistScheduler(WaitlistUseCase useCase) { this.useCase = useCase; }
    @Scheduled(fixedDelayString="${app.waitlist.advance-delay:10000}")
    public void advance() { useCase.advance(); }
}
