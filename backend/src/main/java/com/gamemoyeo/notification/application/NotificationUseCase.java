package com.gamemoyeo.notification.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface NotificationUseCase {
    void publish(Collection<Long> recipients, long meetupId, String kind, String message, String deduplicationKey);
    Page find(long memberId, Long cursor, int size);
    void read(long memberId, long notificationId);
    void dispatch();
    @io.swagger.v3.oas.annotations.media.Schema(name = "NotificationItem")
    record Item(long id, long meetupId, String kind, String message, Instant createdAt, boolean read) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "NotificationPage")
    record Page(List<Item> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long nextCursor, boolean hasNext) { }
}
