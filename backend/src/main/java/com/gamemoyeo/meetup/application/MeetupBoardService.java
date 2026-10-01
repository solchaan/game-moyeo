package com.gamemoyeo.meetup.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionView;
import com.gamemoyeo.meetup.application.port.out.MeetupBoardPort;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import java.time.Duration;
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
    private final ReservationUseCase reservation;

    public MeetupBoardService(MeetupBoardPort port, GameCatalogUseCase catalog, ReservationUseCase reservation) {
        this.port = port;
        this.catalog = catalog;
        this.reservation = reservation;
    }

    @Override
    @Transactional
    public MeetupView create(long ownerId, MeetupCommand command) {
        validate(command);
        MeetupView created = port.create(ownerId, command);
        reservation.initializeOwner(created.id(), ownerId);
        return created;
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
    public CursorPage findAll(Long gameId, Long regionOptionId, Long cursor, int size) {
        if (regionOptionId != null) {
            if (gameId == null) {
                throw invalid("A game is required for a server filter.", "gameId", "서버를 선택하려면 게임을 먼저 선택해 주세요.");
            }
            Map<Long, OptionView> options = new HashMap<>();
            catalog.findGame(gameId).options().forEach(option -> options.put(option.id(), option));
            require(options, regionOptionId, OptionType.REGION, "regionOptionId");
        }
        int pageSize = Math.min(Math.max(size, 1), 100);
        List<MeetupView> values = port.findAll(gameId, regionOptionId, cursor, pageSize + 1);
        boolean hasNext = values.size() > pageSize;
        List<MeetupView> items = hasNext ? values.subList(0, pageSize) : values;
        Long nextCursor = hasNext ? items.get(items.size() - 1).id() : null;
        return new CursorPage(items, nextCursor, hasNext);
    }

    private void validate(MeetupCommand command) {
        if (!command.startsAt().isAfter(Instant.now().plus(Duration.ofMinutes(10)))) {
            throw invalid("Meetup must start more than 10 minutes from now.", "startsAt", "시작 시간은 현재보다 10분 넘게 뒤로 설정해 주세요.");
        }
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw invalid("Meetup time range is invalid.", "endsAt", "종료 시간은 시작 시간보다 뒤로 설정해 주세요.");
        }
        if (command.recruitmentDeadline() != null
            && !command.recruitmentDeadline().isBefore(command.startsAt())) {
            throw invalid("Recruitment deadline must be before start time.", "recruitmentDeadline", "모집 마감 시간은 시작 시간보다 앞으로 설정해 주세요.");
        }
        var game = catalog.findGame(command.gameId());
        if (!game.game().active()) {
            throw invalid("Inactive game cannot be selected.", "gameId", "현재 모집할 수 없는 게임입니다. 다른 게임을 선택해 주세요.");
        }
        Map<Long, OptionView> options = new HashMap<>();
        game.options().forEach(option -> options.put(option.id(), option));
        require(options, command.modeOptionId(), OptionType.MODE, "modeOptionId");
        require(options, command.platformOptionId(), OptionType.PLATFORM, "platformOptionId");
        require(options, command.regionOptionId(), OptionType.REGION, "regionOptionId");
        require(options, command.minimumTierOptionId(), OptionType.TIER, "minimumTierOptionId");
        require(options, command.maximumTierOptionId(), OptionType.TIER, "maximumTierOptionId");
        validateTierRange(options, command.minimumTierOptionId(), command.maximumTierOptionId());
        command.roleRequirements().forEach((id, capacity) -> {
            require(options, id, OptionType.ROLE, "roleRequirements");
            if (capacity == null || capacity < 1) {
                throw invalid("Role capacity must be positive.", "roleRequirements", "역할별 인원은 1명 이상이어야 합니다.");
            }
        });
        int roleCapacity = command.roleRequirements().values().stream().mapToInt(Integer::intValue).sum();
        if (roleCapacity > command.capacity()) {
            throw invalid("Role capacity cannot exceed total capacity.", "roleRequirements", "역할별 인원의 합은 전체 정원을 넘을 수 없습니다.");
        }
    }

    private void require(Map<Long, OptionView> options, Long id, OptionType type, String field) {
        if (id == null) {
            return;
        }
        OptionView option = options.get(id);
        if (option == null || option.type() != type || !option.active()) {
            throw invalid("Invalid or inactive game option.", field, "선택한 게임에서 사용할 수 없는 항목입니다. 다시 선택해 주세요.");
        }
    }

    private void validateTierRange(Map<Long, OptionView> options, Long minimumId, Long maximumId) {
        if (minimumId == null || maximumId == null) {
            return;
        }
        OptionView minimum = options.get(minimumId);
        OptionView maximum = options.get(maximumId);
        if (minimum.sortOrder() > maximum.sortOrder()) {
            throw invalid("Minimum tier cannot be higher than maximum tier.", "minimumTierOptionId", "최소 티어는 최대 티어보다 높을 수 없습니다.");
        }
    }

    private ApiException invalid(String message, String field, String reason) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEETUP_OPTION", message, Map.of(field, reason));
    }
}
