package com.wisecartecommerce.ecommerce.service;

import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.InfoNoteSettingsRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteSettingsResponse;

public interface InfoNoteSettingsService {
    InfoNoteSettingsResponse getSettings();
    InfoNoteSettingsResponse updateSettings(InfoNoteSettingsRequest request);
    InfoNoteSettingsResponse uploadIcon(MultipartFile file);
    int applyNoteToProductsWithoutCustomNote();
    int overwriteNoteOnAllProducts();
    long countProductsWithCustomNote();
}