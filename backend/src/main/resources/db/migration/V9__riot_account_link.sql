CREATE TABLE riot_account (
    member_id BIGINT NOT NULL,
    puuid VARCHAR(191) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    game_name VARCHAR(100) NOT NULL,
    tag_line VARCHAR(32) NOT NULL,
    connected_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (member_id),
    CONSTRAINT uk_riot_account_puuid UNIQUE (puuid),
    CONSTRAINT fk_riot_account_member FOREIGN KEY (member_id) REFERENCES member(id)
) ENGINE=InnoDB;
