-- Add variation_id to order_items only (cart_items already has it from V11)
ALTER TABLE order_items 
ADD COLUMN variation_id BIGINT NULL AFTER product_id;

-- Add foreign key constraint
ALTER TABLE order_items 
ADD CONSTRAINT fk_order_items_variation 
FOREIGN KEY (variation_id) REFERENCES product_variations(id) ON DELETE SET NULL;

-- Add index
CREATE INDEX idx_order_items_variation ON order_items(variation_id);