package com.gamemoyeo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class LeagueServerMigrationTest {
    @Container
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");

    @Test
    void addsNorthAmericaWithoutChangingExistingRegionsOrOtherGames() throws Exception {
        Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()).target("9").load().migrate();
        try (Connection connection = DB.createConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO game (id, slug, name) VALUES (1, 'league-of-legends', 'League'), (2, 'other', 'Other')");
            statement.executeUpdate("INSERT INTO game_option (id, game_id, option_type, code, display_name) VALUES (1, 1, 'REGION', 'KR', 'Custom Korea')");
            var flyway = Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()).load();
            flyway.migrate();
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            try (var rows = statement.executeQuery("SELECT code, display_name FROM game_option WHERE game_id=1 ORDER BY code")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("KR");
                assertThat(rows.getString(2)).isEqualTo("Custom Korea");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("NA");
                assertThat(rows.next()).isFalse();
            }
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM game_option WHERE game_id=2")) {
                rows.next();
                assertThat(rows.getInt(1)).isZero();
            }
        }
    }
}
