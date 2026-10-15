package com.gamemoyeo.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import java.sql.Timestamp;
import java.time.Instant;
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
    @Test
    void persistsDefaultsOverridesAndCancellationWithoutChangingProfile() {
        jdbc.update("INSERT INTO member (id, nickname) VALUES (1, 'owner'), (2, 'participant'), (3, 'other')");
        jdbc.update("INSERT INTO game (id, slug, name) VALUES (1, 'league-of-legends', 'League')");
        jdbc.update("INSERT INTO game_option (id, game_id, option_type, code, display_name) VALUES (1, 1, 'MAP', 'SUMMONERS_RIFT', 'Rift')");
        jdbc.update("INSERT INTO meetup (id, game_id, owner_id, title, map_option_id) VALUES (1, 1, 1, 'Party', 1)");
        jdbc.update("INSERT INTO meetup_session (id, meetup_id, starts_at, capacity, reserved_count) VALUES (1, 1, ?, 3, 1)",
            Timestamp.from(Instant.now().plusSeconds(7200)));
        reservations.initializeOwner(1, 1);
        var profile = new PositionPreferences(Position.MID, Position.SUPPORT);
        assertThat(profiles.find(2, 1)).isEqualTo(PositionPreferences.empty());
        profiles.save(2, 1, profile);
        assertThat(profiles.find(2, 1)).isEqualTo(profile);
        assertThat(profiles.find(3, 1)).isEqualTo(PositionPreferences.empty());
        reservations.participate(1, 2, "default");
        assertThat(savedPrimary()).isEqualTo("MID");
        reservations.cancel(1, 2, "cancel");
        reservations.participate(1, 2, "override", new PositionPreferences(Position.TOP, Position.JUNGLE));
        assertThat(savedPrimary()).isEqualTo("TOP");
        assertThat(profiles.find(2, 1)).isEqualTo(profile);
        reservations.participate(1, 2, "override", new PositionPreferences(Position.TOP, Position.JUNGLE));
        assertThat(jdbc.queryForObject("SELECT reserved_count FROM meetup_session WHERE id=1", Integer.class)).isEqualTo(2);
        reservations.cancel(1, 2, "cancel-again");
        reservations.participate(1, 2, "cleared", PositionPreferences.empty());
        assertThat(savedPrimary()).isNull();
        profiles.save(2, 1, PositionPreferences.empty());
        assertThat(profiles.find(2, 1)).isEqualTo(PositionPreferences.empty());
        assertThatThrownBy(() -> jdbc.update("UPDATE member_game_profile SET primary_position='TOP', secondary_position='TOP' WHERE member_id=2"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    private String savedPrimary() {
        return jdbc.queryForObject("SELECT primary_position FROM reservation WHERE member_id=2 AND session_id=1", String.class);
    }
}
