-- Required for existing databases using spring.jpa.hibernate.ddl-auto=validate.
CREATE TABLE IF NOT EXISTS pending_registration (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_sent_at DATETIME(6) NOT NULL,
    attempts INT NOT NULL,
    send_count INT NOT NULL
);
