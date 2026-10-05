CREATE TABLE saved_address (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 label VARCHAR(80) NOT NULL,
 recipient_name VARCHAR(255) NOT NULL,
 phone VARCHAR(25) NOT NULL,
 address VARCHAR(500) NOT NULL,
 INDEX saved_address_user (user_id)
);
