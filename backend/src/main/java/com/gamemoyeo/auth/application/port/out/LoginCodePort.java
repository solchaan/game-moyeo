package com.gamemoyeo.auth.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface LoginCodePort {

    void save(String code, long memberId, Duration ttl);

    Optional<Long> consume(String code);
}
