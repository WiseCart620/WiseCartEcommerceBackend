ALTER TABLE cart_items DROP INDEX unique_cart_product;

ALTER TABLE cart_items 
ADD CONSTRAINT unique_cart_product_variation 
UNIQUE (cart_id, product_id, variation_id);