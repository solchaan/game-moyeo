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
class LegacyLeagueMapMigrationTest {
    @Container
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");

    @Test
    void restoresKnownModesAndHostWithoutChangingExplicitMapsOrGuestPositions() {
        var source = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(source).target("14").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO member (id,nickname) VALUES (1,'host'),(2,'guest')");
        jdbc.update("INSERT INTO game (id,slug,name) VALUES (1,'league-of-legends','League')");
        jdbc.update("""
            INSERT INTO game_option (id,game_id,option_type,code,display_name) VALUES
            (1,1,'MAP','SUMMONERS_RIFT','Rift'),(2,1,'MAP','HOWLING_ABYSS','ARAM'),
            (3,1,'MODE','RANKED_FLEX','Flex'),(4,1,'MODE','ARAM','ARAM'),(5,1,'MODE','UNKNOWN','Unknown')
            """);
        jdbc.update("INSERT INTO member_game_profile (member_id,game_id,primary_position) VALUES (1,1,'TOP'),(2,1,'MID')");
        for (int id = 1; id <= 5; id++) {
            jdbc.update("INSERT INTO meetup (id,game_id,owner_id,title,mode_option_id,map_option_id) VALUES (?,1,1,'Party',?,?)",
                id, id == 2 ? 4 : id == 3 ? 5 : 3, id == 4 ? 2 : null);
            jdbc.update("INSERT INTO meetup_session (id,meetup_id,starts_at,capacity,reserved_count) VALUES (?,?,UTC_TIMESTAMP(),5,1)", id, id);
            jdbc.update("INSERT INTO reservation (session_id,member_id,primary_position) VALUES (?,1,?)", id, id == 5 ? "SUPPORT" : null);
        }
        jdbc.update("INSERT INTO reservation (session_id,member_id) VALUES (1,2)");
        Flyway.configure().dataSource(source).load().migrate();
        assertThat(jdbc.queryForList("SELECT map_option_id FROM meetup ORDER BY id", Long.class))
            .containsExactly(1L, 2L, null, 2L, 1L);
        assertThat(jdbc.queryForList("SELECT primary_position FROM reservation WHERE member_id=1 ORDER BY session_id", String.class))
            .containsExactly("TOP", null, null, null, "SUPPORT");
        assertThat(jdbc.queryForObject("SELECT primary_position FROM reservation WHERE member_id=2", String.class)).isNull();
    }
}
