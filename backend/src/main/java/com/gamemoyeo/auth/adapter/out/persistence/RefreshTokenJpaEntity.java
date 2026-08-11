package com.gamemoyeo.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenJpaEntity {

    @Id
    @Column(length = 36, columnDefinition = "char(36)")
    private String id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true,
        columnDefinition = "char(64)")
    private String tokenHash;

    @Column(name = "token_family", nullable = false, length = 36, columnDefinition = "char(36)")
    private String tokenFamily;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(
        String id,
        long memberId,
        String tokenHash,
        String tokenFamily,
        Instant issuedAt,
        Instant expiresAt
    ) {
        this.id = id;
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.tokenFamily = tokenFamily;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}
