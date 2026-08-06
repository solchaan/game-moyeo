package com.gamemoyeo.member.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member")
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String nickname;

    private String email;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false, length = 20)
    private String role = "MEMBER";

    protected MemberJpaEntity() {
    }

    public MemberJpaEntity(String nickname, String email, String profileImageUrl) {
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }
}
