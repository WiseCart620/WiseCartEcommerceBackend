-- ============================================================
-- V16__homepage_section_timer.sql
-- Adds configurable countdown timer fields to homepage sections
-- ============================================================

ALTER TABLE homepage_section_configs
    ADD COLUMN show_timer   TINYINT(1)  NOT NULL DEFAULT 0     AFTER display_order,
    ADD COLUMN timer_ends_at DATETIME   NULL                   AFTER show_timer,
    ADD COLUMN timer_label  VARCHAR(60) NULL                   AFTER timer_ends_at;

-- Default: enable the timer on HOT_DEALS only (hidden until admin sets a time)
UPDATE homepage_section_configs
SET    show_timer = 0
WHERE  section_key = 'HOT_DEALS';