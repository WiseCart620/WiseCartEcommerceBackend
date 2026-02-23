package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Request.CategoryRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    
    /**
     * Get all active categories
     */
    List<CategoryResponse> getAllCategories();
    
    /**
     * Get category by ID
     */
    CategoryResponse getCategoryById(Long id);
    
    /**
     * Create new category (Admin only)
     */
    CategoryResponse createCategory(CategoryRequest request);
    
    /**
     * Update existing category (Admin only)
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    
    /**
     * Delete category (Admin only)
     */
    void deleteCategory(Long id);
    
    /**
     * Upload category image (Admin only)
     */
    CategoryResponse uploadCategoryImage(Long id, MultipartFile file);
    
    /**
     * Get category tree (hierarchical structure)
     */
    List<CategoryResponse> getCategoryTree();
    
    /**
     * Get count of products in a category
     */
    Integer getProductCount(Long id);
    
    /**
     * Get category statistics (Admin only)
     */
    Object getCategoryStats();
    
    /**
     * Toggle category status (Admin only)
     */
    CategoryResponse toggleCategoryStatus(Long id, boolean active);
}