package com.gamemoyeo.meetup.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionView;
import com.gamemoyeo.meetup.application.port.out.MeetupBoardPort;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetupBoardService implements MeetupBoardUseCase {

    private final MeetupBoardPort port;
    private final GameCatalogUseCase catalog;

    public MeetupBoardService(MeetupBoardPort port, GameCatalogUseCase catalog) {
        this.port = port;
        this.catalog = catalog;
    }

    @Override
    @Transactional
    public MeetupView create(long ownerId, MeetupCommand command) {
        validate(command);
        return port.create(ownerId, command);
    }

    @Override
    @Transactional
    public MeetupView update(long meetupId, long ownerId, MeetupCommand command) {
        validate(command);
        return port.update(meetupId, ownerId, command);
    }

    @Override
    @Transactional
    public void delete(long meetupId, long ownerId) {
        port.delete(meetupId, ownerId);
    }

    @Override
    public MeetupView find(long meetupId) {
        return port.find(meetupId);
    }

    @Override
    public CursorPage findAll(Long gameId, Long cursor, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        List<MeetupView> values = port.findAll(gameId, cursor, pageSize + 1);
        boolean hasNext = values.size() > pageSize;
        List<MeetupView> items = hasNext ? values.subList(0, pageSize) : values;
        Long nextCursor = hasNext ? items.get(items.size() - 1).id() : null;
        return new CursorPage(items, nextCursor, hasNext);
    }

    private void validate(MeetupCommand command) {
        if (!command.startsAt().isAfter(Instant.now()) || !command.endsAt().isAfter(command.startsAt())) {
            throw invalid("Meetup time range is invalid.");
        }
        if (command.recruitmentDeadline() != null
            && !command.recruitmentDeadline().isBefore(command.startsAt())) {
            throw invalid("Recruitment deadline must be before start time.");
        }
        var game = catalog.findGame(command.gameId());
        if (!game.game().active()) {
            throw invalid("Inactive game cannot be selected.");
        }
        Map<Long, OptionView> options = new HashMap<>();
        game.options().forEach(option -> options.put(option.id(), option));
        require(options, command.modeOptionId(), OptionType.MODE);
        require(options, command.platformOptionId(), OptionType.PLATFORM);
        require(options, command.regionOptionId(), OptionType.REGION);
        require(options, command.minimumTierOptionId(), OptionType.TIER);
        require(options, command.maximumTierOptionId(), OptionType.TIER);
        command.roleRequirements().forEach((id, capacity) -> {
            require(options, id, OptionType.ROLE);
            if (capacity == null || capacity < 1) {
                throw invalid("Role capacity must be positive.");
            }
        });
        int roleCapacity = command.roleRequirements().values().stream().mapToInt(Integer::intValue).sum();
        if (roleCapacity > command.capacity()) {
            throw invalid("Role capacity cannot exceed total capacity.");
        }
    }

    private void require(Map<Long, OptionView> options, Long id, OptionType type) {
        if (id == null) {
            return;
        }
        OptionView option = options.get(id);
        if (option == null || option.type() != type || !option.active()) {
            throw invalid("Invalid or inactive game option: " + id);
        }
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEETUP_OPTION", message);
    }
}
