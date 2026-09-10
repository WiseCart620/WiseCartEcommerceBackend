-- V83__add_info_notes.sql

-- New multi-note table: each row is either "applies to all products"
-- or scoped to specific products via info_note_products
CREATE TABLE info_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note VARCHAR(500) NOT NULL,
    icon_type VARCHAR(20) NOT NULL DEFAULT 'PRESET',
    icon_key VARCHAR(50) DEFAULT 'FiTruck',
    icon_url VARCHAR(500) NULL,
    applies_to_all BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
);

CREATE TABLE info_note_products (
    info_note_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (info_note_id, product_id),
    FOREIGN KEY (info_note_id) REFERENCES info_notes(id) ON DELETE CASCADE
);

-- Carry over the existing global note (if any) from the old singleton table
-- so admins don't lose their current message on upgrade
INSERT INTO info_notes (note, icon_type, icon_key, icon_url, applies_to_all, active, display_order)
SELECT
    COALESCE(NULLIF(note, ''), 'Guaranteed delivery by tomorrow if you order before 12 PM.'),
    COALESCE(icon_type, 'PRESET'),
    COALESCE(icon_key, 'FiTruck'),
    icon_url,
    TRUE,
    TRUE,
    0
FROM info_note_settings
WHERE id = 1;

-- Fallback seed if the old table was empty or didn't exist for some reason
INSERT INTO info_notes (note, icon_type, icon_key, applies_to_all, active, display_order)
SELECT 'Guaranteed delivery by tomorrow if you order before 12 PM.', 'PRESET', 'FiTruck', TRUE, TRUE, 0
WHERE NOT EXISTS (SELECT 1 FROM info_notes);

-- Old singleton table is superseded by info_notes — drop it
DROP TABLE info_note_settings;