package com.gamemoyeo.meetup.adapter.out.persistence;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupView;
import com.gamemoyeo.meetup.application.port.out.MeetupBoardPort;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MeetupBoardPersistenceAdapter implements MeetupBoardPort {

    private final MeetupRepository repository;

    public MeetupBoardPersistenceAdapter(MeetupRepository repository) {
        this.repository = repository;
    }

    @Override
    public MeetupView create(long ownerId, MeetupCommand command) {
        return view(repository.save(new MeetupJpaEntity(ownerId, command)));
    }

    @Override
    public MeetupView update(long meetupId, long ownerId, MeetupCommand command) {
        MeetupJpaEntity meetup = owned(meetupId, ownerId);
        if (meetup.session.reservedCount > command.capacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_BELOW_RESERVED_COUNT",
                "Capacity cannot be lower than current reservations.");
        }
        meetup.update(command);
        return view(meetup);
    }

    @Override
    public void delete(long meetupId, long ownerId) {
        MeetupJpaEntity meetup = owned(meetupId, ownerId);
        meetup.status = "DELETED";
        meetup.session.status = "CLOSED";
    }

    @Override
    public MeetupView find(long meetupId) {
        MeetupJpaEntity meetup = repository.findById(meetupId).orElseThrow(this::notFound);
        if ("DELETED".equals(meetup.status)) {
            throw notFound();
        }
        meetup.roleRequirements.size();
        meetup.session.id.toString();
        return view(meetup);
    }

    @Override
    public List<MeetupView> findAll(Long gameId, Long cursor, int limit) {
        long before = cursor == null ? Long.MAX_VALUE : cursor;
        var page = PageRequest.of(0, limit);
        List<MeetupJpaEntity> meetups = gameId == null
            ? repository.findByStatusAndIdLessThanOrderByIdDesc("OPEN", before, page)
            : repository.findByStatusAndGameIdAndIdLessThanOrderByIdDesc("OPEN", gameId, before, page);
        return meetups.stream().map(this::view).toList();
    }

    private MeetupJpaEntity owned(long meetupId, long ownerId) {
        MeetupJpaEntity meetup = repository.findById(meetupId).orElseThrow(this::notFound);
        if (meetup.ownerId != ownerId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEETUP_OWNER_REQUIRED",
                "Only the meetup owner can modify it.");
        }
        if ("DELETED".equals(meetup.status)) {
            throw notFound();
        }
        return meetup;
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "MEETUP_NOT_FOUND", "Meetup was not found.");
    }

    private MeetupView view(MeetupJpaEntity meetup) {
        MeetupSessionJpaEntity session = meetup.session;
        return new MeetupView(meetup.id, meetup.gameId, meetup.ownerId, meetup.title, meetup.description,
            meetup.modeOptionId, meetup.platformOptionId, meetup.regionOptionId, meetup.minimumTierOptionId,
            meetup.maximumTierOptionId, meetup.playStyle, meetup.voiceChatPolicy, meetup.approvalType,
            meetup.recruitmentDeadline, session.startsAt, session.endsAt, session.capacity, session.reservedCount,
            meetup.status, Map.copyOf(meetup.roleRequirements));
    }
}
