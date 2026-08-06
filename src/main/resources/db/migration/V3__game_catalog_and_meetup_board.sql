ALTER TABLE member
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' AFTER status,
    ADD KEY ix_member_role_status (role, status);

ALTER TABLE game
    ADD COLUMN description VARCHAR(500) NULL AFTER name,
    ADD COLUMN image_url VARCHAR(2048) NULL AFTER description,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;

CREATE TABLE game_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    option_type VARCHAR(30) NOT NULL,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_option_game_type_code (game_id, option_type, code),
    KEY ix_game_option_game_type_active_sort (game_id, option_type, active, sort_order, id),
    CONSTRAINT fk_game_option_game FOREIGN KEY (game_id) REFERENCES game (id)
) ENGINE=InnoDB;

ALTER TABLE meetup
    ADD COLUMN mode_option_id BIGINT NULL AFTER game_id,
    ADD COLUMN platform_option_id BIGINT NULL AFTER mode_option_id,
    ADD COLUMN region_option_id BIGINT NULL AFTER platform_option_id,
    ADD COLUMN minimum_tier_option_id BIGINT NULL AFTER region_option_id,
    ADD COLUMN maximum_tier_option_id BIGINT NULL AFTER minimum_tier_option_id,
    ADD COLUMN play_style VARCHAR(30) NOT NULL DEFAULT 'CASUAL' AFTER description,
    ADD COLUMN voice_chat_policy VARCHAR(30) NOT NULL DEFAULT 'OPTIONAL' AFTER play_style,
    ADD COLUMN approval_type VARCHAR(30) NOT NULL DEFAULT 'FIRST_COME' AFTER voice_chat_policy,
    ADD COLUMN recruitment_deadline DATETIME(6) NULL AFTER approval_type,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER status,
    ADD CONSTRAINT fk_meetup_mode_option FOREIGN KEY (mode_option_id) REFERENCES game_option (id),
    ADD CONSTRAINT fk_meetup_platform_option FOREIGN KEY (platform_option_id) REFERENCES game_option (id),
    ADD CONSTRAINT fk_meetup_region_option FOREIGN KEY (region_option_id) REFERENCES game_option (id),
    ADD CONSTRAINT fk_meetup_minimum_tier_option FOREIGN KEY (minimum_tier_option_id) REFERENCES game_option (id),
    ADD CONSTRAINT fk_meetup_maximum_tier_option FOREIGN KEY (maximum_tier_option_id) REFERENCES game_option (id);

ALTER TABLE meetup_session
    ADD COLUMN ends_at DATETIME(6) NOT NULL AFTER starts_at,
    ADD UNIQUE KEY uk_session_meetup (meetup_id);

CREATE TABLE meetup_role_requirement (
    meetup_id BIGINT NOT NULL,
    role_option_id BIGINT NOT NULL,
    capacity INT NOT NULL,
    PRIMARY KEY (meetup_id, role_option_id),
    CONSTRAINT ck_meetup_role_capacity CHECK (capacity > 0),
    CONSTRAINT fk_meetup_role_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT fk_meetup_role_option FOREIGN KEY (role_option_id) REFERENCES game_option (id)
) ENGINE=InnoDB;
