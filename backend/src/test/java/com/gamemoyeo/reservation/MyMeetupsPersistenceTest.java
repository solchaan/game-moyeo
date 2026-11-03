package com.gamemoyeo.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties="spring.task.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class MyMeetupsPersistenceTest {
    @Container @ServiceConnection static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");
    @Autowired JdbcTemplate jdbc;
    @Autowired MyMeetupsUseCase useCase;
    @Test void filtersMembershipAndTimeBeforePagination() {
        jdbc.update("INSERT INTO member (id,nickname) VALUES (901,'mine'),(902,'other')");
        jdbc.update("INSERT INTO game (id,slug,name) VALUES (901,'my-test','Test')");
        insert(901, 901, "2099-01-01", "OPEN", null);
        insert(902, 902, "2099-01-01", "CLOSED", "CONFIRMED");
        insert(903, 902, "2099-01-01", "OPEN", "CANCELLED");
        insert(904, 902, "2099-01-01", "OPEN", null);
        insert(905, 902, "2000-01-01", "CLOSED", "CONFIRMED");
        insert(906, 901, "2099-01-01", "DELETED", null);
        var first = useCase.find(901, Filter.UPCOMING, null, 1);
        assertThat(first.items()).extracting(MyMeetupsUseCase.Item::id).containsExactly(901L);
        var next = useCase.find(901, Filter.UPCOMING, first.nextCursor(), 1);
        assertThat(next.items()).extracting(MyMeetupsUseCase.Item::id).containsExactly(902L);
        assertThat(next.hasNext()).isFalse();
        assertThat(useCase.find(901, Filter.PAST, null, 20).items())
            .extracting(MyMeetupsUseCase.Item::id).containsExactly(905L);
        var hosted = useCase.find(901, Filter.HOSTED, null, 20).items();
        assertThat(hosted).extracting(MyMeetupsUseCase.Item::id).containsExactly(906L,901L);
        assertThat(hosted.getFirst().canOpen()).isFalse();
        assertThat(hosted.getFirst().canExportCalendar()).isFalse();
    }
    private void insert(long id, long owner, String date, String status, String reservation) {
        jdbc.update("INSERT INTO meetup (id,game_id,owner_id,title,status) VALUES (?,901,?,'Party',?)", id, owner, status);
        jdbc.update("INSERT INTO meetup_session (id,meetup_id,starts_at,capacity) VALUES (?,?,?,5)",id,id,date+" 12:00:00");
        if (reservation != null) {
            jdbc.update("INSERT INTO reservation (session_id,member_id,status) VALUES (?,901,?)",id,reservation);
        }
    }
}
