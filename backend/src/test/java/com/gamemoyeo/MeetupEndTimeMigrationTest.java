package com.gamemoyeo;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MeetupEndTimeMigrationTest {
    @Container
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");

    @Test
    void preservesExistingSessionsAndAllowsSessionsWithoutEndTime() throws Exception {
        Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()).target("10").load().migrate();
        try (var connection = DB.createConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO member (id, nickname) VALUES (1, 'host')");
            statement.executeUpdate("INSERT INTO game (id, slug, name) VALUES (1, 'test', 'Test')");
            statement.executeUpdate("INSERT INTO meetup (id, game_id, owner_id, title) VALUES (1, 1, 1, 'Existing'), (2, 1, 1, 'New')");
            statement.executeUpdate("INSERT INTO meetup_session (meetup_id, starts_at, ends_at, capacity) "
                + "VALUES (1, '2099-01-01 12:00:00', '2099-01-01 13:00:00', 5)");
            Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()).load().migrate();
            statement.executeUpdate("INSERT INTO meetup_session (meetup_id, starts_at, capacity) VALUES (2, '2099-01-01 12:00:00', 5)");
            try (var rows = statement.executeQuery("SELECT meetup_id, ends_at FROM meetup_session ORDER BY meetup_id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getTimestamp("ends_at").toLocalDateTime().toString()).isEqualTo("2099-01-01T13:00");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("meetup_id")).isEqualTo(2L);
                assertThat(rows.getTimestamp("ends_at")).isNull();
                assertThat(rows.next()).isFalse();
            }
        }
    }
}
