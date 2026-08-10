package com.gamemoyeo.auth.application;

import com.gamemoyeo.auth.application.port.out.RefreshTokenPort;
import com.gamemoyeo.auth.application.port.out.MemberAuthorityPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenPort refreshTokenPort;
    private final MemberAuthorityPort memberAuthorityPort;
    private final String issuer;
    private final String audience;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
        JwtEncoder jwtEncoder,
        RefreshTokenPort refreshTokenPort,
        MemberAuthorityPort memberAuthorityPort,
        @Value("${app.token.issuer}") String issuer,
        @Value("${app.token.audience}") String audience,
        @Value("${app.token.access-token-ttl}") Duration accessTtl,
        @Value("${app.token.refresh-token-ttl}") Duration refreshTtl
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenPort = refreshTokenPort;
        this.memberAuthorityPort = memberAuthorityPort;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public TokenPair issue(long memberId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(java.util.List.of(audience))
            .subject(Long.toString(memberId))
            .issuedAt(now)
            .expiresAt(now.plus(accessTtl))
            .id(UUID.randomUUID().toString())
            .claim("roles", java.util.List.of(memberAuthorityPort.findRole(memberId)))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        byte[] random = new byte[48];
        secureRandom.nextBytes(random);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String id = UUID.randomUUID().toString();
        refreshTokenPort.save(id, memberId, sha256(refreshToken), id, now, now.plus(refreshTtl));
        return new TokenPair(accessToken, refreshToken, accessTtl.toSeconds(), "Bearer");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn, String tokenType) {
    }
}
