package com.gamemoyeo.notification.application.port.out;

import com.gamemoyeo.notification.application.NotificationUseCase.Item;
import java.util.List;

public interface NotificationPort {
    void enqueue(long memberId, long meetupId, String kind, String message, String deduplicationKey);
    List<Item> find(long memberId, Long cursor, int limit);
    void read(long memberId, long notificationId);
    void dispatch();
}
