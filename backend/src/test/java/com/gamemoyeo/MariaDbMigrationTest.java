package com.gamemoyeo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MariaDbMigrationTest {

    @Container
    static final MariaDBContainer<?> MARIA_DB = new MariaDBContainer<>("mariadb:11.8");

    @Test
    void migratesReservationSchema() throws Exception {
        var migrationResult = Flyway.configure()
            .dataSource(MARIA_DB.getJdbcUrl(), MARIA_DB.getUsername(), MARIA_DB.getPassword())
            .load()
            .migrate();

        assertThat(migrationResult.targetSchemaVersion).isEqualTo("8");

        try (Connection connection = MARIA_DB.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet queryResult = statement.executeQuery(
                 "SELECT COUNT(*) FROM information_schema.columns "
                     + "WHERE table_schema = DATABASE() AND table_name = 'meetup_session' "
                     + "AND column_name = 'closed_reason'")) {
            assertThat(queryResult.next()).isTrue();
            assertThat(queryResult.getInt(1)).isEqualTo(1);
        }
    }
}
