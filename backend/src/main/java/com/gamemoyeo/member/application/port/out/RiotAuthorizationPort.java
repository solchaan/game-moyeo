package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.domain.RiotAccount;

public interface RiotAuthorizationPort {
    boolean enabled();
    String authorizationUrl(String state, String challenge);
    RiotAccount exchange(String code, String verifier);
}
