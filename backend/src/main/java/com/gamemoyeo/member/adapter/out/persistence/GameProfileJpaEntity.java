package com.gamemoyeo.member.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import com.gamemoyeo.member.domain.PositionPreferences.Position;

@Entity
@Table(name = "member_game_profile")
public class GameProfileJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "member_id", nullable = false) long memberId;
    @Column(name = "game_id", nullable = false) long gameId;
    @Enumerated(EnumType.STRING) @Column(name = "primary_position") Position primaryPosition;
    @Enumerated(EnumType.STRING) @Column(name = "secondary_position") Position secondaryPosition;
    @Column(name = "manual_nickname", length = 64) String manualNickname;
    protected GameProfileJpaEntity() { }
}
