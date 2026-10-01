package com.gamemoyeo.member.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamemoyeo.member.application.port.out.RiotLinkTicketPort;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRiotLinkTicketAdapter implements RiotLinkTicketPort {
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>(
        "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end", Long.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    public RedisRiotLinkTicketAdapter(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }
    @Override
    public void saveState(String key, State value, Duration ttl) {
        save("state:", key, value, ttl);
    }
    @Override
    public Optional<State> consumeState(String key, String browser) {
        return consume("state:", key, State.class, value -> value.browser().equals(browser));
    }
    @Override
    public void saveCompletion(String key, Completion value, Duration ttl) {
        save("completion:", key, value, ttl);
    }
    @Override
    public Optional<Completion> consumeCompletion(String key, long memberId) {
        return consume("completion:", key, Completion.class, value -> value.memberId() == memberId);
    }
    private void save(String kind, String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set("gamemoyeo:riot:" + kind + key, mapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode Riot link state");
        }
    }
    private <T> Optional<T> consume(String kind, String key, Class<T> type, java.util.function.Predicate<T> valid) {
        String redisKey = "gamemoyeo:riot:" + kind + key;
        String encoded = redis.opsForValue().get(redisKey);
        if (encoded == null) {
            return Optional.empty();
        }
        try {
            T value = mapper.readValue(encoded, type);
            if (!valid.test(value) || !Long.valueOf(1).equals(redis.execute(CONSUME, List.of(redisKey), encoded))) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not decode Riot link state");
        }
    }
}
