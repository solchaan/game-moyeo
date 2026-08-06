package com.gamemoyeo.meetup.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface MeetupBoardUseCase {

    MeetupView create(long ownerId, MeetupCommand command);

    MeetupView update(long meetupId, long ownerId, MeetupCommand command);

    void delete(long meetupId, long ownerId);

    MeetupView find(long meetupId);

    CursorPage findAll(Long gameId, Long cursor, int size);

    record MeetupCommand(
        long gameId,
        Long modeOptionId,
        Long platformOptionId,
        Long regionOptionId,
        Long minimumTierOptionId,
        Long maximumTierOptionId,
        String title,
        String description,
        String playStyle,
        String voiceChatPolicy,
        String approvalType,
        Instant recruitmentDeadline,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        Map<Long, Integer> roleRequirements
    ) {
    }

    record MeetupView(
        long id,
        long gameId,
        long ownerId,
        String title,
        String description,
        Long modeOptionId,
        Long platformOptionId,
        Long regionOptionId,
        Long minimumTierOptionId,
        Long maximumTierOptionId,
        String playStyle,
        String voiceChatPolicy,
        String approvalType,
        Instant recruitmentDeadline,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        int reservedCount,
        String status,
        Map<Long, Integer> roleRequirements
    ) {
    }

    record CursorPage(List<MeetupView> items, Long nextCursor, boolean hasNext) {
    }
}
