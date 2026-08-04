ALTER TABLE app_settings
    ADD COLUMN jnt_cod_fee_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0275,
    ADD COLUMN jnt_valuation_fee_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0100,
    ADD COLUMN jnt_valuation_fee_minimum DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    ADD COLUMN jnt_overweight_rate_per_kg DECIMAL(10,2) NOT NULL DEFAULT 70.00,
    ADD COLUMN jnt_overweight_base_fee DECIMAL(10,2) NOT NULL DEFAULT 15.00;