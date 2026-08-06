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
    void migratesInitialSchema() throws Exception {
        Flyway.configure()
            .dataSource(MARIA_DB.getJdbcUrl(), MARIA_DB.getUsername(), MARIA_DB.getPassword())
            .load()
            .migrate();

        try (Connection connection = MARIA_DB.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                 "SELECT COUNT(*) FROM information_schema.tables "
                     + "WHERE table_schema = DATABASE() AND table_name = 'reservation'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }
}
