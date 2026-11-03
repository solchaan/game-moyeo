package com.gamemoyeo.reservation.application;

import java.time.Instant;
import java.util.List;

public interface MyMeetupsUseCase {
    Page find(long memberId, Filter filter, String cursor, int size);
    enum Filter { UPCOMING, HOSTED, PAST }
    @io.swagger.v3.oas.annotations.media.Schema(name = "MyMeetupItem")
    record Item(long id, long gameId, String title, Instant startsAt, String status,
        String participationStatus, int participantCount, int capacity, boolean canOpen, boolean canExportCalendar, boolean ready, boolean canSetReady) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "MyMeetupPage")
    record Page(List<Item> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) String nextCursor, boolean hasNext) { }
}
