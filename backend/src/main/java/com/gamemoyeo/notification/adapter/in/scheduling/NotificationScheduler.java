package com.gamemoyeo.notification.adapter.in.scheduling;

import com.gamemoyeo.notification.application.NotificationUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="spring.task.scheduling.enabled", havingValue="true", matchIfMissing=true)
public class NotificationScheduler {
    private final NotificationUseCase useCase;
    public NotificationScheduler(NotificationUseCase useCase) { this.useCase = useCase; }
    @Scheduled(fixedDelayString = "${app.notification.dispatch-delay:5000}")
    public void dispatch() { useCase.dispatch(); }
}
