ALTER TABLE meetup
    ADD COLUMN map_option_id BIGINT NULL,
    ADD CONSTRAINT fk_meetup_map_option FOREIGN KEY (map_option_id) REFERENCES game_option(id);
