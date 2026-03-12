package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Request.CategoryRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.CategoryResponse;
import com.wisecartecommerce.ecommerce.entity.Category;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.CategoryRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.service.CategoryService;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all active categories");
        List<Category> categories = categoryRepository.findAllActive();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Object getCategoryStats() {
        log.info("Fetching category statistics");

        Map<String, Object> stats = new HashMap<>();
        long totalCategories = categoryRepository.count();
        long activeCategories = categoryRepository.countByActiveTrue();
        long inactiveCategories = categoryRepository.countByActiveFalse();
        long categoriesWithProducts = categoryRepository.countCategoriesWithProducts();
        long rootCategories = categoryRepository.countByParentIsNull();
        List<Object[]> categoriesByLevel = categoryRepository.countCategoriesByLevel();
        List<Object[]> topCategoriesByProducts = categoryRepository.findTopCategoriesByProductCount(5);

        stats.put("totalCategories", totalCategories);
        stats.put("activeCategories", activeCategories);
        stats.put("inactiveCategories", inactiveCategories);
        stats.put("categoriesWithProducts", categoriesWithProducts);
        stats.put("rootCategories", rootCategories);
        stats.put("categoriesByLevel", categoriesByLevel);
        stats.put("topCategoriesByProducts", topCategoriesByProducts);

        return stats;
    }

    @Override
    public CategoryResponse toggleCategoryStatus(Long id, boolean active) {
        log.info("Toggling category status for ID: {} to active: {}", id, active);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        category.setActive(active);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Category status updated successfully for ID: {}", id);

        return mapToResponse(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.isActive())
                .build();

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());

        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        categoryRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
            }
        });

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setActive(request.isActive());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: {}", id);

        return mapToResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        Long productCount = productRepository.countProductsByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category with " + productCount + " products. " +
                    "Please reassign or delete products first.");
        }

        List<Category> children = categoryRepository.findByParentIdAndActiveTrue(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete category with subcategories. " +
                    "Please delete or reassign subcategories first.");
        }

        category.setActive(false);
        categoryRepository.save(category);

        log.info("Category deleted successfully: {}", id);
    }

    @Override
    public CategoryResponse uploadCategoryImage(Long id, MultipartFile file) {
        log.info("Uploading image for category ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            try {
                fileStorageService.deleteFile(category.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete old category image: {}", e.getMessage());
            }
        }

        try {
            String imageUrl = fileStorageService.uploadFile(file, "categories");
            category.setImageUrl(imageUrl);
        } catch (IOException e) {
            log.error("Failed to upload category image", e);
            throw new RuntimeException("Failed to upload category image: " + e.getMessage(), e);
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category image uploaded successfully for ID: {}", id);

        return mapToResponse(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        log.info("Fetching category tree");

        List<Category> rootCategories = categoryRepository.findByParentIsNullAndActiveTrue();

        return rootCategories.stream()
                .map(this::mapToResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getProductCount(Long id) {
        log.info("Getting product count for category ID: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }

        return productRepository.countProductsByCategoryId(id).intValue();
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();

        if (category.getParent() != null) {
            response.setParentId(category.getParent().getId());
            response.setParentName(category.getParent().getName());
        }

        response.setProductCount(productRepository.countProductsByCategoryId(category.getId()).intValue());

        return response;
    }

    private CategoryResponse mapToResponseWithChildren(Category category) {
        CategoryResponse response = mapToResponse(category);

        List<Category> children = categoryRepository.findByParentIdAndActiveTrue(category.getId());
        if (!children.isEmpty()) {
            List<CategoryResponse> childResponses = children.stream()
                    .map(this::mapToResponseWithChildren)
                    .collect(Collectors.toList());
            response.setChildren(childResponses);
        }

        return response;
    }
}