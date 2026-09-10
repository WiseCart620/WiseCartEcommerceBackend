-- V82__add_info_note_settings.sql
CREATE TABLE info_note_settings (
    id BIGINT PRIMARY KEY,
    note VARCHAR(500) NULL,
    icon_type VARCHAR(20) NOT NULL DEFAULT 'PRESET',
    icon_key VARCHAR(50) NULL DEFAULT 'FiTruck',
    icon_url VARCHAR(500) NULL,
    updated_at DATETIME NULL
);

INSERT INTO info_note_settings (id, note, icon_type, icon_key)
VALUES (1, 'Guaranteed delivery by tomorrow if you order before 12 PM.', 'PRESET', 'FiTruck');