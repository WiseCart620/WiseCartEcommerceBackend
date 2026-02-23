package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String slug;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> children;
    private Integer productCount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}