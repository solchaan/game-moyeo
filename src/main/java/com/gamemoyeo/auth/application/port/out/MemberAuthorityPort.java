package com.gamemoyeo.auth.application.port.out;

public interface MemberAuthorityPort {

    String findRole(long memberId);
}
