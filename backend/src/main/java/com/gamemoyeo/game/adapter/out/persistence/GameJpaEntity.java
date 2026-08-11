package com.gamemoyeo.game.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "game")
class GameJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 80)
    String slug;
    @Column(nullable = false, length = 120)
    String name;
    @Column(length = 500)
    String description;
    @Column(name = "image_url", length = 2048)
    String imageUrl;
    @Column(nullable = false, length = 20)
    String status = "ACTIVE";

    protected GameJpaEntity() {
    }

    GameJpaEntity(String slug, String name, String description, String imageUrl) {
        update(slug, name, description, imageUrl);
    }

    void update(String slug, String name, String description, String imageUrl) {
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
