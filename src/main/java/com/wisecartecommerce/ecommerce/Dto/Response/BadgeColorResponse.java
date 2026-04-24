package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeColorResponse {
    private Long id;
    private String badgeName;
    private String colorClass;
    private boolean active;
    private int displayOrder;
}