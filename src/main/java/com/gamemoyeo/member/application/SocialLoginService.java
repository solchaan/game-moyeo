package com.gamemoyeo.member.application;

import com.gamemoyeo.member.application.port.out.SocialAccountPort;
import com.gamemoyeo.member.application.port.out.SocialAccountPort.MemberAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialLoginService {

    private final SocialAccountPort socialAccountPort;

    public SocialLoginService(SocialAccountPort socialAccountPort) {
        this.socialAccountPort = socialAccountPort;
    }

    @Transactional
    public MemberAccount login(SocialProfile profile) {
        return socialAccountPort.find(profile.provider(), profile.subject())
            .map(account -> {
                socialAccountPort.recordLogin(profile.provider(), profile.subject(), profile);
                return account;
            })
            .orElseGet(() -> socialAccountPort.create(profile, defaultNickname(profile)));
    }

    private String defaultNickname(SocialProfile profile) {
        String prefix = profile.provider().toLowerCase();
        String digest;
        try {
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((profile.provider() + ':' + profile.subject()).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
        return (prefix + '_' + digest.substring(0, 12)).substring(0, Math.min(40, prefix.length() + 13));
    }
}
