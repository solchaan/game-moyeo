CREATE TABLE reservation_waitlist (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    meetup_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    offered_until DATETIME(6) NULL,
    UNIQUE KEY uk_waitlist_member (meetup_id,member_id),
    KEY ix_waitlist_queue (meetup_id,status,joined_at,id),
    CONSTRAINT fk_waitlist_meetup FOREIGN KEY(meetup_id) REFERENCES meetup(id),
    CONSTRAINT fk_waitlist_member FOREIGN KEY(member_id) REFERENCES member(id)
) ENGINE=InnoDB;
