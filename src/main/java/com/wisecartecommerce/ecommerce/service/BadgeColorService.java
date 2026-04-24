package com.wisecartecommerce.ecommerce.service;

import java.util.List;

import com.wisecartecommerce.ecommerce.Dto.Request.BadgeColorRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.BadgeColorResponse;

public interface BadgeColorService {
    List<BadgeColorResponse> getAllBadgeColors();
    List<BadgeColorResponse> getActiveBadgeColors();
    BadgeColorResponse createOrUpdateBadgeColor(BadgeColorRequest request);
    void deleteBadgeColor(Long id);
    BadgeColorResponse updateBadgeColor(Long id, BadgeColorRequest request);
}