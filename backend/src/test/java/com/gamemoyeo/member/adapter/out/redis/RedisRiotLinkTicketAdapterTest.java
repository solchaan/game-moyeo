package com.gamemoyeo.member.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamemoyeo.member.application.port.out.RiotLinkTicketPort;
import com.gamemoyeo.member.domain.RiotAccount;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisRiotLinkTicketAdapterTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8-alpine")).withExposedPorts(6379);
    @Test
    void enforcesBrowserMemberExpiryAndAtomicSingleUse() throws Exception {
        var factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        try {
            var template = new StringRedisTemplate(factory);
            var adapter = new RedisRiotLinkTicketAdapter(template, new ObjectMapper());
            adapter.saveState("state", new RiotLinkTicketPort.State(7, "browser", "verifier"), Duration.ofMinutes(5));
            assertThat(adapter.consumeState("state", "other-browser")).isEmpty();
            assertThat(adapter.consumeState("state", "browser")).isPresent();
            assertThat(adapter.consumeState("state", "browser")).isEmpty();
            adapter.saveCompletion("ticket", new RiotLinkTicketPort.Completion(7, new RiotAccount("id", "Player", "KR1")), Duration.ofMinutes(1));
            assertThat(adapter.consumeCompletion("ticket", 8)).isEmpty();
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> adapter.consumeCompletion("ticket", 7));
                var second = executor.submit(() -> adapter.consumeCompletion("ticket", 7));
                assertThat((first.get().isPresent() ? 1 : 0) + (second.get().isPresent() ? 1 : 0)).isEqualTo(1);
            }
            adapter.saveState("expired", new RiotLinkTicketPort.State(7, "browser", "verifier"), Duration.ofMinutes(5));
            template.expire("gamemoyeo:riot:state:expired", Duration.ZERO);
            assertThat(adapter.consumeState("expired", "browser")).isEmpty();
        } finally {
            factory.destroy();
        }
    }
}
