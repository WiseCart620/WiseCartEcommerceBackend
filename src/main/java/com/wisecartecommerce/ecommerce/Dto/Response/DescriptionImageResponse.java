package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionImageResponse {
    private Long id;
    private String imageUrl;
    private String altText;
    private String fileName;
}