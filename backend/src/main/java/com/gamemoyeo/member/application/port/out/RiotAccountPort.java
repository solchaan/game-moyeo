package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.domain.RiotAccount;
import java.util.Optional;

public interface RiotAccountPort {
    Optional<RiotAccount> find(long memberId);
    void link(long memberId, RiotAccount account);
    void unlink(long memberId);
}
