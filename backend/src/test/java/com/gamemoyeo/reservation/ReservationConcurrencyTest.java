package com.gamemoyeo.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
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
class ReservationConcurrencyTest {

    private static final int CAPACITY = 11;
    private static final int CONTENDER_COUNT = 60;

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> MARIA_DB = new MariaDBContainer<>("mariadb:11.8");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ReservationUseCase reservation;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO member (id, nickname) VALUES (1, 'owner')");
        for (long memberId = 2; memberId <= CONTENDER_COUNT + 1; memberId++) {
            jdbc.update("INSERT INTO member (id, nickname) VALUES (?, ?)", memberId, "member-" + memberId);
        }
        jdbc.update("INSERT INTO game (id, slug, name) VALUES (1, 'concurrency-game', 'Concurrency Game')");
        jdbc.update("INSERT INTO meetup (id, game_id, owner_id, title) VALUES (1, 1, 1, 'Concurrent meetup')");
        Instant startsAt = Instant.now().plus(2, ChronoUnit.HOURS);
        jdbc.update("""
            INSERT INTO meetup_session
                (id, meetup_id, starts_at, ends_at, capacity, reserved_count)
            VALUES (1, 1, ?, ?, ?, 1)
            """, Timestamp.from(startsAt), Timestamp.from(startsAt.plus(1, ChronoUnit.HOURS)), CAPACITY);
        jdbc.update("""
            INSERT INTO reservation (session_id, member_id, party_size, status)
            VALUES (1, 1, 1, 'CONFIRMED')
            """);
    }

    @Test
    void neverOverbooksWithSixtyConcurrentParticipationRequests() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(CONTENDER_COUNT);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (long memberId = 2; memberId <= CONTENDER_COUNT + 1; memberId++) {
                long contenderId = memberId;
                results.add(executor.submit(() -> participate(start, contenderId)));
            }
            start.countDown();

            int accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(20, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }

            assertThat(accepted).isEqualTo(CAPACITY - 1);
            assertThat(jdbc.queryForObject(
                "SELECT reserved_count FROM meetup_session WHERE id = 1", Integer.class)).isEqualTo(CAPACITY);
            assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM reservation
                WHERE session_id = 1 AND status = 'CONFIRMED'
                """, Integer.class)).isEqualTo(CAPACITY);
            assertThat(jdbc.queryForObject(
                "SELECT status FROM meetup_session WHERE id = 1", String.class)).isEqualTo("CLOSED");
            assertThat(jdbc.queryForObject(
                "SELECT closed_reason FROM meetup_session WHERE id = 1", String.class)).isEqualTo("FULL");
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean participate(CountDownLatch start, long memberId) throws InterruptedException {
        start.await();
        try {
            reservation.participate(1L, memberId, "concurrent-" + memberId);
            return true;
        } catch (ApiException exception) {
            return false;
        }
    }
}
