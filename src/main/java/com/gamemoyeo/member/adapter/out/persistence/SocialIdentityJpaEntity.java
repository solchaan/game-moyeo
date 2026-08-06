package com.gamemoyeo.member.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import com.gamemoyeo.member.application.SocialProfile;

@Entity
@Table(name = "social_identity", uniqueConstraints = {
    @UniqueConstraint(name = "uk_social_identity_provider_subject",
        columnNames = {"provider", "provider_subject"})
})
public class SocialIdentityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 191)
    private String providerSubject;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    protected SocialIdentityJpaEntity() {
    }

    public SocialIdentityJpaEntity(MemberJpaEntity member, SocialProfile profile, Instant now) {
        this.member = member;
        this.provider = profile.provider();
        this.providerSubject = profile.subject();
        this.providerEmail = profile.email();
        this.emailVerified = profile.emailVerified();
        this.displayName = profile.displayName();
        this.profileImageUrl = profile.profileImageUrl();
        this.connectedAt = now;
        this.lastLoginAt = now;
    }

    public MemberJpaEntity getMember() {
        return member;
    }

    public void recordLogin(SocialProfile profile, Instant now) {
        providerEmail = profile.email();
        emailVerified = profile.emailVerified();
        displayName = profile.displayName();
        profileImageUrl = profile.profileImageUrl();
        lastLoginAt = now;
    }
}
