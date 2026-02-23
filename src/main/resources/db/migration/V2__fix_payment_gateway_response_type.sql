-- Change gateway_response column from TEXT to TINYTEXT to match Hibernate expectations
ALTER TABLE payments MODIFY COLUMN gateway_response TINYTEXT;