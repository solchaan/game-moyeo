CREATE TABLE meetup_template (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    source_meetup_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    payload JSON NOT NULL,
    UNIQUE KEY uk_template_member_source(member_id,source_meetup_id),
    KEY ix_template_member_id(member_id,id),
    CONSTRAINT fk_template_member FOREIGN KEY(member_id) REFERENCES member(id)
) ENGINE=InnoDB;
