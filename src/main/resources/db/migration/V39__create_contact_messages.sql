CREATE TABLE contact_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT          NULL,
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    subject     VARCHAR(200)    NOT NULL,
    message     TEXT            NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_contact_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_contact_user_id  (user_id),
    INDEX idx_contact_status   (status),
    INDEX idx_contact_created  (created_at),
    INDEX idx_contact_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contact_replies (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    contact_message_id  BIGINT          NOT NULL,
    sender_type         VARCHAR(10)     NOT NULL,
    message             TEXT            NOT NULL,
    is_read             TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reply_contact FOREIGN KEY (contact_message_id)
        REFERENCES contact_messages(id) ON DELETE CASCADE,
    INDEX idx_reply_message_id  (contact_message_id),
    INDEX idx_reply_is_read     (is_read),
    INDEX idx_reply_created     (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;