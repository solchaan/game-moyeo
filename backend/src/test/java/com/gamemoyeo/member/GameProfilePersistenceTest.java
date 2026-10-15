package com.gamemoyeo.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import com.gamemoyeo.reservation.application.PositionAvailabilityUseCase;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class GameProfilePersistenceTest {
    @Container @ServiceConnection
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");
    @Autowired JdbcTemplate jdbc;
    @Autowired GameProfileUseCase profiles;
    @Autowired ReservationUseCase reservations;
    @Autowired PositionAvailabilityUseCase availability;
    @Autowired MeetupBoardUseCase meetups;
    @Test
    void persistsDefaultsOverridesAndCancellationWithoutChangingProfile() {
        jdbc.update("INSERT INTO member (id, nickname) VALUES (1, 'owner'), (2, 'participant'), (3, 'other')");
        jdbc.update("INSERT INTO game (id, slug, name) VALUES (1, 'league-of-legends', 'League')");
        jdbc.update("INSERT INTO game_option (id, game_id, option_type, code, display_name) VALUES (1, 1, 'MAP', 'SUMMONERS_RIFT', 'Rift')");
        jdbc.update("""
            INSERT INTO game_option (id, game_id, option_type, code, display_name) VALUES
            (2, 1, 'ROLE', 'TOP', 'Top'), (3, 1, 'ROLE', 'BOTTOM', 'Bottom'),
            (4, 1, 'ROLE', 'SUPPORT', 'Support'), (5, 1, 'ROLE', 'MID', 'Mid')
            """);
        profiles.save(1, 1, new PositionPreferences(Position.TOP, Position.SUPPORT));
        var created = meetups.create(1, new MeetupBoardUseCase.MeetupCommand(1, null, null, null, null, null,
            "Party", "", "CASUAL", "OPTIONAL", "FIRST_COME", null, Instant.now().plusSeconds(7200),
            5, Map.of(2L, 2, 3L, 1, 4L, 1, 5L, 1), 1L));
        assertThat(created.id()).isEqualTo(1);
        profiles.save(1, 1, new PositionPreferences(Position.JUNGLE, null));
        var initial = availability.find(1);
        assertThat(initial.ownerPositions().primaryPosition()).isEqualTo(Position.TOP);
        assertThat(initial.positions().get(0).includesOwner()).isTrue();
        assertThat(initial.positions().get(0).participantCount()).isEqualTo(1);
        assertThat(initial.positions().get(0).remainingCapacity()).isEqualTo(1);
        assertThat(initial.positions().get(3).position()).isEqualTo("ADC");
        assertThat(initial.positions().get(3).capacity()).isEqualTo(1);
        assertThat(initial.positions().get(4).participantCount()).isZero();
        var profile = new PositionPreferences(Position.MID, Position.SUPPORT);
        assertThat(profiles.find(2, 1)).isEqualTo(PositionPreferences.empty());
        profiles.save(2, 1, profile);
        assertThat(profiles.find(2, 1)).isEqualTo(profile);
        assertThat(profiles.find(3, 1)).isEqualTo(PositionPreferences.empty());
        reservations.participate(1, 2, "default");
        assertThat(savedPrimary()).isEqualTo("MID");
        assertThat(availability.find(1).positions().get(2).status()).isEqualTo("FILLED");
        reservations.cancel(1, 2, "cancel");
        assertThat(availability.find(1).positions().get(2).participantCount()).isZero();
        reservations.participate(1, 2, "override", new PositionPreferences(Position.TOP, Position.JUNGLE));
        assertThat(savedPrimary()).isEqualTo("TOP");
        assertThat(availability.find(1).positions().get(0).remainingCapacity()).isZero();
        assertThat(profiles.find(2, 1)).isEqualTo(profile);
        reservations.participate(1, 2, "override", new PositionPreferences(Position.TOP, Position.JUNGLE));
        assertThat(jdbc.queryForObject("SELECT reserved_count FROM meetup_session WHERE id=1", Integer.class)).isEqualTo(2);
        reservations.cancel(1, 2, "cancel-again");
        reservations.participate(1, 2, "cleared", PositionPreferences.empty());
        assertThat(savedPrimary()).isNull();
        assertThat(availability.find(1).unassignedCount()).isEqualTo(1);
        reservations.participate(1, 3, "any-position", new PositionPreferences(Position.ANY, Position.MID));
        assertThat(availability.find(1).anyPositionCount()).isEqualTo(1);
        assertThat(availability.find(1).positions().get(2).participantCount()).isZero();
        reservations.close(1, 1, "owner-closes");
        assertThat(availability.find(1).positions()).allMatch(slot -> slot.status().equals("CLOSED"));
        profiles.save(2, 1, PositionPreferences.empty());
        assertThat(profiles.find(2, 1)).isEqualTo(PositionPreferences.empty());
        assertThatThrownBy(() -> jdbc.update("UPDATE member_game_profile SET primary_position='TOP', secondary_position='TOP' WHERE member_id=2"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    private String savedPrimary() {
        return jdbc.queryForObject("SELECT primary_position FROM reservation WHERE member_id=2 AND session_id=1", String.class);
    }
}
