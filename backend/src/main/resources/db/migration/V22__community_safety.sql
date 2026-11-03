CREATE TABLE member_block (
    member_id BIGINT NOT NULL,
    blocked_member_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY(member_id,blocked_member_id),
    KEY ix_block_reverse(blocked_member_id,member_id),
    CONSTRAINT fk_block_member FOREIGN KEY(member_id) REFERENCES member(id),
    CONSTRAINT fk_block_target FOREIGN KEY(blocked_member_id) REFERENCES member(id),
    CONSTRAINT ck_block_self CHECK(member_id<>blocked_member_id)
) ENGINE=InnoDB;
CREATE TABLE member_endorsement (
    member_id BIGINT NOT NULL,
    target_member_id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    PRIMARY KEY(member_id,target_member_id,meetup_id),
    CONSTRAINT fk_endorse_member FOREIGN KEY(member_id) REFERENCES member(id),
    CONSTRAINT fk_endorse_target FOREIGN KEY(target_member_id) REFERENCES member(id),
    CONSTRAINT fk_endorse_meetup FOREIGN KEY(meetup_id) REFERENCES meetup(id)
) ENGINE=InnoDB;
CREATE TABLE member_report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_member_id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution VARCHAR(2000) NULL,
    resolved_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6) NULL,
    UNIQUE KEY uk_report_context(reporter_id,target_member_id,meetup_id),
    KEY ix_report_status_id(status,id),
    CONSTRAINT fk_report_member FOREIGN KEY(reporter_id) REFERENCES member(id),
    CONSTRAINT fk_report_target FOREIGN KEY(target_member_id) REFERENCES member(id),
    CONSTRAINT fk_report_meetup FOREIGN KEY(meetup_id) REFERENCES meetup(id),
    CONSTRAINT fk_report_resolver FOREIGN KEY(resolved_by) REFERENCES member(id)
) ENGINE=InnoDB;
