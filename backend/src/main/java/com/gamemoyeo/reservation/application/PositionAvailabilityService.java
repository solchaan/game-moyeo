package com.gamemoyeo.reservation.application;

import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase;
import com.gamemoyeo.member.application.port.in.PositionSelection;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import com.gamemoyeo.reservation.application.port.out.PositionRosterPort;
import com.gamemoyeo.reservation.domain.PositionVacancy;
import java.util.EnumMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PositionAvailabilityService implements PositionAvailabilityUseCase {
    private final MeetupBoardUseCase meetups;
    private final GameCatalogUseCase games;
    private final ReservationUseCase reservations;
    private final PositionRosterPort roster;
    private final com.gamemoyeo.member.application.port.in.GameProfileUseCase profiles;

    public PositionAvailabilityService(MeetupBoardUseCase meetups, GameCatalogUseCase games,
        ReservationUseCase reservations, PositionRosterPort roster,
        com.gamemoyeo.member.application.port.in.GameProfileUseCase profiles) {
        this.meetups = meetups;
        this.games = games;
        this.reservations = reservations;
        this.roster = roster;
        this.profiles = profiles;
    }

    @Override
    public PositionAvailabilityView find(long meetupId) {
        var meetup = meetups.find(meetupId);
        var game = games.findGame(meetup.gameId());
        var participation = reservations.state(meetupId, null);
        boolean enabled = game.positionsAllowed(meetup.modeOptionId(), meetup.mapOptionId());
        if (!enabled) {
            return new PositionAvailabilityView(false, false, participation.remainingCapacity(), 0, 0,
                PositionSelection.from(PositionPreferences.empty()), List.of());
        }
        var targets = new EnumMap<Position, Integer>(Position.class);
        game.options().stream().filter(option -> option.type() == OptionType.ROLE).forEach(option -> {
            Integer capacity = meetup.roleRequirements().get(option.id());
            if (capacity != null) {
                Position position = switch (option.code()) {
                    case "TOP" -> Position.TOP;
                    case "JUNGLE" -> Position.JUNGLE;
                    case "MID" -> Position.MID;
                    case "BOTTOM", "ADC" -> Position.ADC;
                    case "SUPPORT" -> Position.SUPPORT;
                    default -> null;
                };
                if (position != null) {
                    targets.merge(position, capacity, Integer::sum);
                }
            }
        });
        var members = roster.confirmedPositions(meetupId);
        var nicknames = profiles.nicknames(meetup.gameId(), members.stream().map(PositionRosterPort.MemberPosition::memberId).toList());
        var owner = members.stream().filter(member -> member.memberId() == meetup.ownerId())
            .map(PositionRosterPort.MemberPosition::positions).findFirst().orElse(PositionPreferences.empty());
        var slots = List.of(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT).stream()
            .map(position -> {
                int count = (int) members.stream().filter(member -> member.positions().primaryPosition() == position).count();
                var vacancy = PositionVacancy.calculate(count, targets.get(position), participation.remainingCapacity(),
                    "OPEN".equals(participation.recruitmentStatus()));
                return new PositionSlot(position.name(), count, vacancy.capacity(), vacancy.remainingCapacity(),
                    vacancy.status(), owner.primaryPosition() == position,
                    members.stream().filter(member -> member.positions().primaryPosition() == position)
                        .map(member -> nicknames.getOrDefault(member.memberId(), "")).toList());
            }).toList();
        return new PositionAvailabilityView(true, !targets.isEmpty(), participation.remainingCapacity(),
            (int) members.stream().filter(member -> member.positions().primaryPosition() == null).count(),
            (int) members.stream().filter(member -> member.positions().primaryPosition() == Position.ANY).count(),
            PositionSelection.from(owner), slots);
    }
}
