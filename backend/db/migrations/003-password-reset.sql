-- Apply once to an existing production database before schema validation.
ALTER TABLE `user` ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS password_reset (
    email VARCHAR(255) NOT NULL PRIMARY KEY,
    reset_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    password_hash VARCHAR(255) NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_sent_at DATETIME(6) NOT NULL,
    attempts INT NOT NULL,
    send_count INT NOT NULL
);
