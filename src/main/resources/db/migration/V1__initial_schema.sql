CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_subject VARCHAR(128) NOT NULL,
    nickname VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_external_subject (external_subject),
    UNIQUE KEY uk_member_nickname (nickname)
) ENGINE=InnoDB;

CREATE TABLE game (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_slug (slug)
) ENGINE=InnoDB;

CREATE TABLE meetup (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_meetup_game_status_id (game_id, status, id),
    KEY ix_meetup_owner_id (owner_id, id),
    CONSTRAINT fk_meetup_game FOREIGN KEY (game_id) REFERENCES game (id),
    CONSTRAINT fk_meetup_owner FOREIGN KEY (owner_id) REFERENCES member (id)
) ENGINE=InnoDB;

CREATE TABLE meetup_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meetup_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    capacity INT NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_session_meetup_starts (meetup_id, starts_at, id),
    KEY ix_session_status_starts (status, starts_at, id),
    CONSTRAINT ck_session_capacity CHECK (capacity > 0),
    CONSTRAINT ck_session_reserved_count CHECK (reserved_count >= 0 AND reserved_count <= capacity),
    CONSTRAINT fk_session_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id)
) ENGINE=InnoDB;

CREATE TABLE reservation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    party_size INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    cancelled_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_session_member (session_id, member_id),
    KEY ix_reservation_member_status_id (member_id, status, id),
    CONSTRAINT ck_reservation_party_size CHECK (party_size > 0),
    CONSTRAINT fk_reservation_session FOREIGN KEY (session_id) REFERENCES meetup_session (id),
    CONSTRAINT fk_reservation_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;

CREATE TABLE idempotency_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status INT NULL,
    response_body JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_member_operation_key (member_id, operation, idempotency_key),
    KEY ix_idempotency_expires_at (expires_at),
    CONSTRAINT fk_idempotency_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;

CREATE TABLE outbox_event (
    id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSON NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY ix_outbox_unpublished (published_at, occurred_at),
    CONSTRAINT ck_outbox_attempts CHECK (attempts >= 0)
) ENGINE=InnoDB;
