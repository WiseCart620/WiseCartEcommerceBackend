package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.Review;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByProductId(Long productId);
    
    Page<Review> findByProductId(Long productId, Pageable pageable);
    
    List<Review> findByUserId(Long userId);
    
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    
    Optional<Review> findByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND " +
           "(:minRating IS NULL OR r.rating >= :minRating) AND " +
           "(:maxRating IS NULL OR r.rating <= :maxRating) AND " +
           "r.active = true ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    Page<Review> findProductReviewsWithFilters(
            @Param("productId") Long productId,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            Pageable pageable);
    
    @Query("SELECT AVG(r.rating) as avgRating, COUNT(r) as totalReviews, " +
           "SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END) as fiveStar, " +
           "SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END) as fourStar, " +
           "SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END) as threeStar, " +
           "SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END) as twoStar, " +
           "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) as oneStar " +
           "FROM Review r WHERE r.product.id = :productId AND r.active = true")
    Object getProductReviewSummary(@Param("productId") Long productId);
    
    @Query("SELECT r FROM Review r WHERE r.active = true ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
}