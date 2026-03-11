package com.wisecartecommerce.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.ProductAddOn;

import jakarta.transaction.Transactional;

@Repository
public interface ProductAddOnRepository extends JpaRepository<ProductAddOn, Long> {
    List<ProductAddOn> findByProductIdOrderByDisplayOrderAsc(Long productId);

    boolean existsByProductIdAndAddOnProductId(Long productId, Long addOnProductId);

    @Modifying
    @Transactional
    void deleteByProductIdAndId(Long productId, Long id);
}
