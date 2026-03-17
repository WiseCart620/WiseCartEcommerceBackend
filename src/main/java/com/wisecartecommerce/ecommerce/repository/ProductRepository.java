package com.wisecartecommerce.ecommerce.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByActiveTrue();

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    List<Product> findByFeaturedTrueAndActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.active = true AND "
            + "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND "
            + "(:categoryId IS NULL OR p.category.id = :categoryId) AND "
            + "(:minPrice IS NULL OR p.price >= :minPrice) AND "
            + "(:maxPrice IS NULL OR p.price <= :maxPrice) AND "
            + "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
            + "(:inStock IS NULL OR (:inStock = true AND p.stockQuantity > 0) OR (:inStock = false AND p.stockQuantity = 0)) AND "
            + "(:onSale IS NULL OR (:onSale = true AND p.discount > 0) OR (:onSale = false AND p.discount = 0))")
    Page<Product> findActiveProductsWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            @Param("inStock") Boolean inStock,
            @Param("onSale") Boolean onSale,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= :threshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.soldCount DESC")
    List<Product> findTopSellingProducts(Pageable pageable);

    @Query("SELECT p, COALESCE(SUM(oi.quantity), 0) as totalSold, COALESCE(SUM(oi.price * oi.quantity), 0) as totalRevenue "
            + "FROM Product p "
            + "LEFT JOIN p.orderItems oi "
            + "LEFT JOIN oi.order o "
            + "WHERE p.active = true AND "
            + "(o IS NULL OR (o.status = 'DELIVERED' AND "
            + "(:startDate IS NULL OR o.createdAt >= :startDate) AND "
            + "(:endDate IS NULL OR o.createdAt <= :endDate))) "
            + "GROUP BY p.id, p.name, p.price, p.sku, p.stockQuantity, p.soldCount, p.active "
            + "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProductsByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.createdAt DESC")
    List<Product> findNewArrivals(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.discount > 0")
    Page<Product> findProductsOnSale(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :productId AND p.active = true")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId, @Param("productId") Long productId,
            Pageable pageable);

    @Query("SELECT p.name FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.active = true")
    List<String> findProductNameSuggestions(@Param("query") String query, Pageable pageable);

    @Query("SELECT SUM(p.soldCount) FROM Product p")
    Long getTotalSoldCount();

    @Query("SELECT AVG(p.rating) FROM Product p WHERE p.reviewCount > 0")
    BigDecimal getAverageRating();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
