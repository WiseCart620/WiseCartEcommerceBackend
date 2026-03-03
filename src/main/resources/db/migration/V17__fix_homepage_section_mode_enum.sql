-- Fix: homepage_section_configs.mode column was created as ENUM in V15
-- but Hibernate expects VARCHAR to match @Enumerated(EnumType.STRING)
ALTER TABLE homepage_section_configs
    MODIFY COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'AUTO';

-- Normalize any existing lowercase values to uppercase to match Java enum names
-- (AUTO, MANUAL, CATEGORY)
UPDATE homepage_section_configs
SET mode = UPPER(mode)
WHERE mode != UPPER(mode);