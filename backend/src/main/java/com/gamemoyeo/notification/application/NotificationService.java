package com.gamemoyeo.notification.application;

import com.gamemoyeo.notification.application.port.out.NotificationPort;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService implements NotificationUseCase {
    private final NotificationPort port;
    public NotificationService(NotificationPort port) { this.port = port; }
    @Override
    public void publish(Collection<Long> recipients, long meetupId, String kind, String message, String deduplicationKey) {
        recipients.stream().distinct().forEach(member -> port.enqueue(member, meetupId, kind, message, deduplicationKey));
    }
    @Override
    @Transactional(readOnly = true)
    public Page find(long memberId, Long cursor, int size) {
        int limit = Math.min(Math.max(size, 1), 100);
        var rows = port.find(memberId, cursor, limit + 1);
        boolean more = rows.size() > limit;
        var items = List.copyOf(rows.subList(0, Math.min(rows.size(), limit)));
        return new Page(items, more ? items.getLast().id() : null, more);
    }
    @Override
    public void read(long memberId, long notificationId) { port.read(memberId, notificationId); }
    @Override
    public void dispatch() { port.dispatch(); }
}
