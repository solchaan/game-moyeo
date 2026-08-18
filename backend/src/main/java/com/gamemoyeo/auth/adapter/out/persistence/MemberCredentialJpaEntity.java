package com.gamemoyeo.auth.adapter.out.persistence;

import com.gamemoyeo.member.adapter.out.persistence.MemberJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "member_credential")
public class MemberCredentialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private MemberJpaEntity member;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Version
    private long version;

    protected MemberCredentialJpaEntity() {
    }

    public MemberCredentialJpaEntity(
        MemberJpaEntity member,
        String username,
        String passwordHash,
        Instant now
    ) {
        this.member = member;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
    }

    public Long getId() {
        return id;
    }

    public MemberJpaEntity getMember() {
        return member;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void recordFailure(int failures, Instant until) {
        failedAttempts = failures;
        lockedUntil = until;
    }

    public void recordSuccess(Instant now) {
        failedAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }
}
