package com.gamemoyeo.meetup.application;

import java.time.Instant;
import java.util.List;

public interface MeetupTemplateUseCase {
    long save(long memberId, long sourceMeetupId);
    Page find(long memberId, Long cursor, int size);
    void delete(long memberId, long templateId);
    Created create(long memberId, long templateId, Instant startsAt, int weeks, String timeZone, String key);
    @io.swagger.v3.oas.annotations.media.Schema(name = "MeetupTemplateItem")
    record Item(long id, String title) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "MeetupTemplatePage")
    record Page(List<Item> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long nextCursor, boolean hasNext) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "CreatedMeetups")
    record Created(List<Long> meetupIds) { }
}
