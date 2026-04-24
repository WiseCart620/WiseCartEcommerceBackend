package com.wisecartecommerce.ecommerce.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeColorRequest {
    private Long id;
    private String badgeName;
    private String colorClass;
    private boolean active;
    private int displayOrder;
}