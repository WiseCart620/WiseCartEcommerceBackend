package com.wisecartecommerce.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.ProductImage;
import com.wisecartecommerce.ecommerce.entity.ProductImage.ImageType;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    List<ProductImage> findByProductIdAndImageType(Long productId, ImageType imageType);
    void deleteByProductId(Long productId);
}