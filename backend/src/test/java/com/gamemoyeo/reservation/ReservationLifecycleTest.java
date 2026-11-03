package com.gamemoyeo.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.notification.application.NotificationUseCase;
import com.gamemoyeo.reservation.application.ReservationUseCase;
import org.junit.jupiter.api.BeforeEach;
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
class ReservationLifecycleTest {
    @Container
    @ServiceConnection
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationUseCase reservations;
    @Autowired NotificationUseCase notifications;
    @Autowired com.gamemoyeo.meetup.application.MeetupTemplateUseCase templates;
    @Autowired com.gamemoyeo.member.application.CommunityUseCase community;
    @Autowired com.gamemoyeo.reservation.application.RecommendationUseCase recommendations;
    @Autowired com.gamemoyeo.reservation.application.WaitlistUseCase waitlist;

    @BeforeEach
    void setup() {
        jdbc.update("INSERT INTO member(id,nickname) VALUES (701,'lifecycle-host'),(702,'lifecycle-guest'),(703,'outsider')");
        jdbc.update("INSERT INTO game(id,slug,name) VALUES (701,'lifecycle','Lifecycle')");
        jdbc.update("INSERT INTO meetup(id,game_id,owner_id,title) VALUES (701,701,701,'Party')");
        jdbc.update("INSERT INTO meetup_session(id,meetup_id,starts_at,capacity,reserved_count) VALUES (701,701,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 20 MINUTE),5,1)");
        jdbc.update("INSERT INTO reservation(session_id,member_id) VALUES (701,701)");
    }
    @Test
    void templatesCreateIndependentWeeklyMeetupsAndReplaySameResult() {
        long template = templates.save(701,701);
        var start = java.time.Instant.parse("2099-04-01T12:00:00Z");
        var result = templates.create(701,template,start,3,"Asia/Seoul","repeat");
        assertThat(result.meetupIds()).hasSize(3);
        assertThat(templates.create(701,template,start,3,"Asia/Seoul","repeat")).isEqualTo(result);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meetup",Integer.class)).isEqualTo(4);
        assertThatThrownBy(() -> templates.create(702,template,start,1,"Asia/Seoul","intruder"))
            .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> templates.create(701,template,start,2,"Asia/Seoul","repeat"))
            .isInstanceOf(ApiException.class);
        for (long id : result.meetupIds()) {
            assertThat(reservations.state(id,701L).participantCount()).isEqualTo(1);
        }
    }
    @Test
    void blockIsBidirectionalForAdmissionAndFeedbackRequiresSharedPast() {
        community.block(701,702,true);
        assertThatThrownBy(() -> reservations.participate(701,702,"blocked")).isInstanceOf(ApiException.class);
        community.block(701,702,false);
        community.block(702,701,true);
        assertThatThrownBy(() -> reservations.participate(701,702,"reverse-blocked")).isInstanceOf(ApiException.class);
        community.block(702,701,false);
        reservations.participate(701,702,"join");
        assertThatThrownBy(() -> community.endorse(701,702,701,true)).isInstanceOf(ApiException.class);
        jdbc.update("UPDATE meetup_session SET starts_at='2000-01-01' WHERE id=701");
        community.endorse(701,702,701,true);
        community.report(701,702,701,"ABUSE","Repeated abuse");
        community.report(701,702,701,"ABUSE","Repeated abuse");
        assertThat(community.companions(701,null,20).items().getFirst().endorsed()).isTrue();
        assertThat(community.reports(null,20).items()).hasSize(1);
        assertThatThrownBy(() -> community.report(703,702,701,"ABUSE","No attendance"))
            .isInstanceOf(ApiException.class);
        community.resolve(701,community.reports(null,20).items().getFirst().id(),"DISMISSED","Checked attendance");
        assertThat(community.reports(null,20).items().getFirst().status()).isEqualTo("DISMISSED");
    }
    @Test
    void recommendationsFilterAndExplainPositionVacanciesBeforePagination() {
        jdbc.update("UPDATE game SET slug='league-of-legends' WHERE id=701");
        jdbc.update("INSERT INTO member_game_profile(member_id,game_id,primary_position,secondary_position) VALUES (702,701,'MID','SUPPORT')");
        jdbc.update("INSERT INTO game_option(id,game_id,option_type,code,display_name) VALUES (701,701,'ROLE','MID','Mid'),(702,701,'ROLE','SUPPORT','Support')");
        jdbc.update("INSERT INTO meetup_role_requirement(meetup_id,role_option_id,capacity) VALUES (701,701,1)");
        var query = new com.gamemoyeo.reservation.application.RecommendationUseCase.Query(701,null,null,null,null,null,null,null,1);
        var page = recommendations.find(702,query);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().reason()).isEqualTo("PRIMARY_POSITION");
        jdbc.update("UPDATE reservation SET primary_position='MID' WHERE member_id=701");
        assertThat(recommendations.find(702,query).items().getFirst().reason()).isEqualTo("FILTER_MATCH");
        assertThat(recommendations.find(701,query).items()).isEmpty();
    }
    @Test
    void waitingOrderProtectsVacancyAndExpiredOfferAdvances() {
        jdbc.update("INSERT INTO member(id,nickname) VALUES (704,'waiting-first'),(705,'waiting-second')");
        jdbc.update("UPDATE meetup_session SET capacity=2 WHERE id=701");
        reservations.participate(701,702,"fill");
        assertThat(waitlist.join(701,704).position()).isEqualTo(1);
        assertThat(waitlist.join(701,705).position()).isEqualTo(2);
        reservations.cancel(701,702,"release");
        assertThat(waitlist.find(701,704).canAccept()).isTrue();
        assertThatThrownBy(() -> reservations.participate(701,703,"bypass")).isInstanceOf(ApiException.class).hasMessageContaining("priority");
        jdbc.update("UPDATE reservation_waitlist SET offered_until=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 SECOND) WHERE member_id=704");
        waitlist.advance();
        assertThat(waitlist.find(701,704).canAccept()).isFalse();
        assertThat(waitlist.find(701,705).canAccept()).isTrue();
        reservations.participate(701,705,"accept");
        assertThat(waitlist.find(701,705).status()).isEqualTo("ACCEPTED");
        assertThat(reservations.state(701,705L).participantCount()).isEqualTo(2);
    }
    @Test
    void cancellingOfferPromotesNextWaitingMember() {
        jdbc.update("UPDATE meetup_session SET capacity=2 WHERE id=701");
        reservations.participate(701,702,"fill");
        waitlist.join(701,703);
        reservations.cancel(701,702,"release");
        assertThat(waitlist.cancel(701,703).status()).isEqualTo("CANCELLED");
        reservations.participate(701,702,"return");
        assertThat(reservations.state(701,702L).participantCount()).isEqualTo(2);
    }
    @Test
    void replayRemainsAvailableAfterMeetupDeletion() {
        var original = reservations.participate(701,702,"join");
        jdbc.update("UPDATE meetup SET status='DELETED' WHERE id=701");
        assertThat(reservations.participate(701,702,"join")).isEqualTo(original);
        assertThatThrownBy(() -> reservations.participate(701,703,"new")).isInstanceOf(ApiException.class);
    }
    @Test
    void invalidNotificationIsQuarantinedWithoutBlockingValidEvents() {
        jdbc.update("INSERT INTO outbox_event(id,aggregate_type,aggregate_id,event_type,payload,occurred_at) VALUES ('invalid-notification','meetup','701','MEMBER_NOTIFICATION','{\"memberId\":999999,\"meetupId\":701,\"kind\":\"TEST\",\"message\":\"bad\"}',UTC_TIMESTAMP())");
        reservations.participate(701,702,"join");
        for (int attempt=0;attempt<6;attempt++) { notifications.dispatch(); }
        assertThat(notifications.find(702,null,20).items()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_event WHERE id='invalid-notification'",Integer.class)).isEqualTo(5);
    }
    @Test
    void replayCannotRejoinAfterCancellationAndDoesNotDuplicateEvents() {
        var joined = reservations.participate(701,702,"join");
        reservations.cancel(701,702,"cancel");
        assertThat(reservations.participate(701,702,"join")).isEqualTo(joined);
        assertThat(reservations.state(701,702L).participationStatus()).isEqualTo("NOT_JOINED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event", Integer.class)).isEqualTo(4);
        assertThatThrownBy(() -> reservations.participate(701,702,"join", PositionPreferences.empty()))
            .isInstanceOf(ApiException.class).hasMessageContaining("different input");
        assertThat(jdbc.queryForObject("SELECT reserved_count FROM meetup_session WHERE id=701", Integer.class)).isEqualTo(1);
    }
    @Test
    void dispatchAndRemindersAreIdempotentAndReadStateIsPrivate() {
        reservations.participate(701,702,"join");
        reservations.remindUpcoming();
        reservations.remindUpcoming();
        notifications.dispatch();
        notifications.dispatch();
        var inbox = notifications.find(702,null,20);
        assertThat(inbox.items()).hasSize(2);
        assertThat(inbox.items()).extracting(NotificationUseCase.Item::kind).containsExactlyInAnyOrder("PARTICIPATION","REMINDER");
        long id = inbox.items().getFirst().id();
        notifications.read(703,id);
        assertThat(notifications.find(702,null,20).items().getFirst().read()).isFalse();
        notifications.read(702,id);
        assertThat(notifications.find(702,null,20).items().getFirst().read()).isTrue();
        assertThat(notifications.find(703,null,20).items()).isEmpty();
    }
    @Test
    void onlyConfirmedUpcomingParticipantsCanSetReadyAndCancelResetsIt() {
        assertThatThrownBy(() -> reservations.ready(701,703,true)).isInstanceOf(ApiException.class);
        reservations.participate(701,702,"join");
        reservations.ready(701,702,true);
        assertThat(jdbc.queryForObject("SELECT ready FROM reservation WHERE member_id=702", Boolean.class)).isTrue();
        reservations.cancel(701,702,"cancel");
        assertThat(jdbc.queryForObject("SELECT ready FROM reservation WHERE member_id=702", Boolean.class)).isFalse();
    }
}
