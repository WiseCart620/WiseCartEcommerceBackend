-- V43__add_section_banner_to_homepage_sections.sql

ALTER TABLE homepage_section_configs
    ADD COLUMN section_banner_url  VARCHAR(500) NULL,
    ADD COLUMN section_banner_link VARCHAR(500) NULL;