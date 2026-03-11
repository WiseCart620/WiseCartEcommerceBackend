-- Add-ons table
CREATE TABLE product_add_ons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  add_on_product_id BIGINT NOT NULL,
  special_price DECIMAL(10,2),
  display_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_product_addon (product_id, add_on_product_id),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (add_on_product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Hand-picked recommendations
CREATE TABLE product_recommendations (
  product_id BIGINT NOT NULL,
  recommended_product_id BIGINT NOT NULL,
  display_order INT DEFAULT 0,
  PRIMARY KEY (product_id, recommended_product_id),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (recommended_product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Recommendation category + marketplace links on product
ALTER TABLE products
  ADD COLUMN lazada_url VARCHAR(2048),
  ADD COLUMN shopee_url VARCHAR(2048),
  ADD COLUMN recommendation_category_id BIGINT,
  ADD FOREIGN KEY (recommendation_category_id) REFERENCES categories(id) ON DELETE SET NULL;