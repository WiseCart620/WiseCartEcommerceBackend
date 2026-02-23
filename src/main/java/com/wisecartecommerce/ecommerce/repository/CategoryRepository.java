package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    Optional<Category> findBySlug(String slug);
    
    List<Category> findByParentIsNullAndActiveTrue();
    
    List<Category> findByParentIdAndActiveTrue(Long parentId);
    
    List<Category> findByActiveTrue();
    
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.name ASC")
    List<Category> findAllActive();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true ORDER BY c.name ASC")
    List<Category> findRootCategories();
    
    @Query("SELECT c FROM Category c WHERE c.active = true AND " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Category> searchCategories(@Param("search") String search);
    
    // ============ ADD THESE METHODS FOR STATISTICS ============
    
    long countByActiveTrue();
    long countByActiveFalse();
    long countByParentIsNull();
    
    @Query("SELECT COUNT(DISTINCT c) FROM Category c WHERE SIZE(c.products) > 0")
    long countCategoriesWithProducts();
    
    @Query("SELECT c.level, COUNT(c) FROM Category c GROUP BY c.level ORDER BY c.level")
    List<Object[]> countCategoriesByLevel();
    
    @Query("SELECT c.name, COUNT(p) as productCount FROM Category c LEFT JOIN c.products p GROUP BY c.id ORDER BY productCount DESC")
    List<Object[]> findAllCategoriesWithProductCount();
    
    @Query(value = "SELECT c.name, COUNT(p.id) as productCount FROM categories c LEFT JOIN products p ON c.id = p.category_id GROUP BY c.id ORDER BY productCount DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopCategoriesByProductCount(@Param("limit") int limit);
    
    boolean existsByName(String name);
}