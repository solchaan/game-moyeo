package com.gamemoyeo.member.application;

public record SocialProfile(
    String provider,
    String subject,
    String email,
    boolean emailVerified,
    String displayName,
    String profileImageUrl
) {
}
