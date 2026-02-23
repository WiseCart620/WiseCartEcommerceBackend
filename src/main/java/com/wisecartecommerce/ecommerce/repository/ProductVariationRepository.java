package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariationRepository extends JpaRepository<ProductVariation, Long> {
    List<ProductVariation> findByProductId(Long productId);
    Optional<ProductVariation> findBySku(String sku);
    Optional<ProductVariation> findByUpc(String upc);
    boolean existsBySku(String sku);
    boolean existsByUpc(String upc);
}