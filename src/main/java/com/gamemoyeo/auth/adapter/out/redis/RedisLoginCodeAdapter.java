package com.gamemoyeo.auth.adapter.out.redis;

import com.gamemoyeo.auth.application.port.out.LoginCodePort;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisLoginCodeAdapter implements LoginCodePort {

    private static final String PREFIX = "auth:login-code:";
    private static final DefaultRedisScript<String> GET_AND_DELETE =
        new DefaultRedisScript<>("local v=redis.call('GET',KEYS[1]); "
            + "if v then redis.call('DEL',KEYS[1]); end; return v", String.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLoginCodeAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String code, long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + code, Long.toString(memberId), ttl);
    }

    @Override
    public Optional<Long> consume(String code) {
        String value = redisTemplate.execute(GET_AND_DELETE, java.util.List.of(PREFIX + code));
        return Optional.ofNullable(value).map(Long::valueOf);
    }
}
