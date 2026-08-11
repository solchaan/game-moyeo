package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.application.SocialProfile;
import java.util.Optional;

public interface SocialAccountPort {

    Optional<MemberAccount> find(String provider, String subject);

    MemberAccount create(SocialProfile profile, String nickname);

    void recordLogin(String provider, String subject, SocialProfile profile);

    record MemberAccount(long memberId, String status) {
    }
}
