ALTER TABLE member DROP INDEX uk_member_external_subject;
ALTER TABLE member DROP COLUMN external_subject;
ALTER TABLE member ADD COLUMN email VARCHAR(254) NULL AFTER nickname;
ALTER TABLE member ADD COLUMN profile_image_url VARCHAR(2048) NULL AFTER email;

CREATE TABLE social_identity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(191) NOT NULL,
    provider_email VARCHAR(254) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(100) NULL,
    profile_image_url VARCHAR(2048) NULL,
    connected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_social_identity_provider_subject (provider, provider_subject),
    UNIQUE KEY uk_social_identity_member_provider (member_id, provider),
    KEY ix_social_identity_member (member_id),
    CONSTRAINT fk_social_identity_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;

CREATE TABLE refresh_token (
    id CHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    token_family CHAR(36) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY ix_refresh_token_member_expires (member_id, expires_at),
    KEY ix_refresh_token_family (token_family),
    CONSTRAINT fk_refresh_token_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;
