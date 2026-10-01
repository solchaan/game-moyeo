package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.domain.RiotAccount;
import java.time.Duration;
import java.util.Optional;

public interface RiotLinkTicketPort {
    void saveState(String key, State value, Duration ttl);
    Optional<State> consumeState(String key, String browser);
    void saveCompletion(String key, Completion value, Duration ttl);
    Optional<Completion> consumeCompletion(String key, long memberId);
    record State(long memberId, String browser, String verifier) { }
    record Completion(long memberId, RiotAccount account) { }
}
