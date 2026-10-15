package com.gamemoyeo.meetup.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamemoyeo.meetup.application.MeetupBoardUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class ServerFilterPersistenceTest {
    @Container
    @ServiceConnection
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MeetupBoardUseCase board;

    @Test
    void createsAndUpdatesMeetupWithoutEndTime() {
        jdbc.update("INSERT INTO member (id, nickname) VALUES (901, 'schedule-host')");
        jdbc.update("INSERT INTO game (id, slug, name) VALUES (901, 'schedule-test', 'Schedule test')");
        var start = java.time.Instant.parse("2099-01-01T12:00:00Z");
        var command = new MeetupBoardUseCase.MeetupCommand(901L, null, null, null, null, null,
            "Start only", null, "CASUAL", "OPTIONAL", "FIRST_COME", null, start, 5, java.util.Map.of(), null);
        var created = board.create(901L, command);
        assertThat(board.find(created.id()).startsAt()).isEqualTo(start);
        var updated = new MeetupBoardUseCase.MeetupCommand(901L, null, null, null, null, null,
            "Updated", null, "CASUAL", "OPTIONAL", "FIRST_COME", null, start.plusSeconds(900), 5, java.util.Map.of(), null);
        board.update(created.id(), 901L, updated);
        assertThat(board.find(created.id()).startsAt()).isEqualTo(start.plusSeconds(900));
        assertThat(jdbc.queryForObject("SELECT ends_at FROM meetup_session WHERE meetup_id = ?",
            java.sql.Timestamp.class, created.id())).isNull();
    }

    @Test
    void isolatesServersBeforePaginationAndPreservesUnassignedParties() {
        jdbc.update("INSERT INTO member (id, nickname) VALUES (801, 'server-host')");
        jdbc.update("INSERT INTO game (id, slug, name) VALUES (801, 'server-test', 'Server test'), (802, 'other-server-test', 'Other')");
        jdbc.update("INSERT INTO game_option (id, game_id, option_type, code, display_name) VALUES "
            + "(801, 801, 'REGION', 'KR', 'Korea'), (802, 801, 'REGION', 'NA', 'North America'), "
            + "(803, 802, 'REGION', 'NA', 'Other North America')");
        insert(801, 801, 801L, "OPEN");
        insert(802, 801, 802L, "OPEN");
        insert(803, 801, null, "OPEN");
        insert(804, 801, 802L, "OPEN");
        insert(805, 801, 802L, "DELETED");
        insert(806, 802, 803L, "OPEN");
        var first = board.findAll(801L, 802L, null, 1);
        assertThat(first.items()).extracting(MeetupBoardUseCase.MeetupView::id).containsExactly(804L);
        assertThat(first.hasNext()).isTrue();
        var next = board.findAll(801L, 802L, first.nextCursor(), 1);
        assertThat(next.items()).extracting(MeetupBoardUseCase.MeetupView::id).containsExactly(802L);
        assertThat(next.hasNext()).isFalse();
        assertThat(board.findAll(801L, 801L, null, 20).items())
            .extracting(MeetupBoardUseCase.MeetupView::id).containsExactly(801L);
        assertThat(board.findAll(801L, null, null, 20).items())
            .extracting(MeetupBoardUseCase.MeetupView::id).containsExactly(804L, 803L, 802L, 801L);
    }

    private void insert(long id, long gameId, Long region, String status) {
        jdbc.update("INSERT INTO meetup (id, game_id, owner_id, title, region_option_id, status) VALUES (?, ?, 801, 'Party', ?, ?)",
            id, gameId, region, status);
        jdbc.update("INSERT INTO meetup_session (meetup_id, starts_at, capacity) VALUES (?, '2099-01-01 12:00:00', 5)", id);
    }
}
