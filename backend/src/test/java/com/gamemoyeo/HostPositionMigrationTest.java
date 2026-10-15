package com.gamemoyeo;

import static org.assertj.core.api.Assertions.assertThat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class HostPositionMigrationTest {
    @Container
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");

    @Test
    void backfillsOnlyEmptyConfirmedHostPositionsOnRift() {
        var source = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(source).target("13").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO member (id,nickname) VALUES (1,'host'),(2,'guest')");
        jdbc.update("INSERT INTO game (id,slug,name) VALUES (1,'league-of-legends','League')");
        jdbc.update("""
            INSERT INTO game_option (id,game_id,option_type,code,display_name) VALUES
            (1,1,'MAP','SUMMONERS_RIFT','Rift'),(2,1,'MAP','HOWLING_ABYSS','ARAM')
            """);
        jdbc.update("INSERT INTO member_game_profile (member_id,game_id,primary_position,secondary_position) VALUES (1,1,'TOP','SUPPORT'),(2,1,'MID',NULL)");
        for (int id = 1; id <= 4; id++) {
            jdbc.update("INSERT INTO meetup (id,game_id,owner_id,title,map_option_id) VALUES (?,1,1,'Party',?)", id, id == 2 ? 2 : 1);
            jdbc.update("INSERT INTO meetup_session (id,meetup_id,starts_at,capacity,reserved_count) VALUES (?,?,UTC_TIMESTAMP(),5,1)", id, id);
            jdbc.update("INSERT INTO reservation (session_id,member_id,status,primary_position) VALUES (?,1,?,?)",
                id, id == 4 ? "CANCELLED" : "CONFIRMED", id == 3 ? "ADC" : null);
        }
        jdbc.update("INSERT INTO reservation (session_id,member_id) VALUES (1,2)");
        Flyway.configure().dataSource(source).load().migrate();
        assertThat(jdbc.queryForObject("SELECT primary_position FROM reservation WHERE session_id=1 AND member_id=1", String.class)).isEqualTo("TOP");
        assertThat(jdbc.queryForObject("SELECT secondary_position FROM reservation WHERE session_id=1 AND member_id=1", String.class)).isEqualTo("SUPPORT");
        assertThat(jdbc.queryForObject("SELECT primary_position FROM reservation WHERE session_id=1 AND member_id=2", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT primary_position FROM reservation WHERE session_id=2", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT primary_position FROM reservation WHERE session_id=3", String.class)).isEqualTo("ADC");
        assertThat(jdbc.queryForObject("SELECT primary_position FROM reservation WHERE session_id=4", String.class)).isNull();
    }
}
