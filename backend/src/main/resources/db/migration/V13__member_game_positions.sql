CREATE TABLE member_game_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    primary_position VARCHAR(20) NULL,
    secondary_position VARCHAR(20) NULL,
    UNIQUE KEY uk_member_game_profile (member_id, game_id),
    CONSTRAINT fk_profile_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_profile_game FOREIGN KEY (game_id) REFERENCES game(id),
    CONSTRAINT ck_profile_positions CHECK (secondary_position IS NULL OR (primary_position IS NOT NULL AND primary_position <> secondary_position))
) ENGINE=InnoDB;
ALTER TABLE reservation
    ADD COLUMN primary_position VARCHAR(20) NULL,
    ADD COLUMN secondary_position VARCHAR(20) NULL;
