ALTER TABLE notifications
    ADD COLUMN image_url    VARCHAR(1000) NULL,
    ADD COLUMN total_items  INT           NULL;