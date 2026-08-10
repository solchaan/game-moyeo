CREATE TABLE admin_credential (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    password_changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_credential_member (member_id),
    UNIQUE KEY uk_admin_credential_username (username),
    CONSTRAINT ck_admin_failed_attempts CHECK (failed_attempts >= 0),
    CONSTRAINT fk_admin_credential_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;
