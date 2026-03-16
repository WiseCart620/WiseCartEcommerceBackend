ALTER TABLE app_settings
    ADD COLUMN cart_enabled     TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN buy_now_enabled  TINYINT(1) NOT NULL DEFAULT 1;