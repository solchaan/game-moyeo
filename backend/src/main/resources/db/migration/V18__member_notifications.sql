CREATE TABLE member_notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id CHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    kind VARCHAR(40) NOT NULL,
    message VARCHAR(240) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at DATETIME(6) NULL,
    UNIQUE KEY uk_notification_event (event_id),
    KEY ix_notification_member_id (member_id, id),
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member(id)
) ENGINE=InnoDB;
