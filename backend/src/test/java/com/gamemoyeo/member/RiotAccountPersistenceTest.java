package com.gamemoyeo.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.out.RiotAccountPort;
import com.gamemoyeo.member.domain.RiotAccount;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RiotAccountPersistenceTest {
    @Container
    @ServiceConnection
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.8");
    @Autowired
    RiotAccountPort accounts;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactions;

    @Test
    void onlyOneMemberCanClaimAnAccountAndCannotSilentlyReplaceIt() throws Exception {
        jdbc.update("INSERT INTO member (id, nickname) VALUES (701, 'riot-one'), (702, 'riot-two')");
        var tx = new TransactionTemplate(transactions);
        var gate = new CountDownLatch(1);
        var account = new RiotAccount("shared-puuid", "Player", "KR1");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claim(701, account, gate));
            var second = executor.submit(() -> claim(702, account, gate));
            gate.countDown();
            assertThat((first.get(15, TimeUnit.SECONDS) ? 1 : 0) + (second.get(15, TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
        }
        Long winner = jdbc.queryForObject("SELECT member_id FROM riot_account WHERE puuid = 'shared-puuid'", Long.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> accounts.link(winner, new RiotAccount("different", "Other", "KR1"))))
            .isInstanceOf(ApiException.class);
        tx.executeWithoutResult(s -> accounts.link(winner, new RiotAccount("shared-puuid", "Renamed", "KR2")));
        assertThat(accounts.find(winner).orElseThrow().riotId()).isEqualTo("Renamed#KR2");
        tx.executeWithoutResult(s -> accounts.unlink(winner));
        tx.executeWithoutResult(s -> accounts.unlink(winner));
        assertThat(accounts.find(winner)).isEmpty();
    }
    private boolean claim(long memberId, RiotAccount account, CountDownLatch gate) throws InterruptedException {
        gate.await();
        try {
            new TransactionTemplate(transactions).executeWithoutResult(s -> accounts.link(memberId, account));
            return true;
        } catch (ApiException exception) {
            assertThat(exception.code()).isEqualTo("RIOT_ACCOUNT_ALREADY_LINKED");
            return false;
        }
    }
}
