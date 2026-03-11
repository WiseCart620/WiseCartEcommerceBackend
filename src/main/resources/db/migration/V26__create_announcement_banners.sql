CREATE TABLE announcement_banners (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    text           VARCHAR(500)    NOT NULL,
    link           VARCHAR(500)    NULL,
    bg_color       VARCHAR(50)     NOT NULL DEFAULT '#1e40af',
    text_color     VARCHAR(50)     NOT NULL DEFAULT '#ffffff',
    active         TINYINT(1)      NOT NULL DEFAULT 1,
    display_order  INT             NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_announcement_banners_active (active),
    INDEX idx_announcement_banners_order  (display_order)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;