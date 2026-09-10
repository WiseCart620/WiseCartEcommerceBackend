package com.wisecartecommerce.ecommerce.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.InfoNoteSettingsRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteSettingsResponse;
import com.wisecartecommerce.ecommerce.entity.InfoNoteSettings;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.InfoNoteSettingsRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.InfoNoteSettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfoNoteSettingsServiceImpl implements InfoNoteSettingsService {

    private final InfoNoteSettingsRepository repository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    private InfoNoteSettings getOrCreate() {
        return repository.findById(1L).orElseGet(() -> {
            InfoNoteSettings s = InfoNoteSettings.builder().id(1L).build();
            return repository.save(s);
        });
    }

    @Override
    @Cacheable("infoNoteSettings")
    @Transactional(readOnly = true)
    public InfoNoteSettingsResponse getSettings() {
        return mapToResponse(getOrCreate());
    }

    @Override
    @Transactional
    @CacheEvict(value = "infoNoteSettings", allEntries = true)
    public InfoNoteSettingsResponse updateSettings(InfoNoteSettingsRequest request) {
        InfoNoteSettings settings = getOrCreate();
        settings.setNote(request.getNote());
        if (request.getIconType() != null) {
            settings.setIconType(request.getIconType());
        }
        if ("PRESET".equalsIgnoreCase(settings.getIconType())) {
            settings.setIconKey(request.getIconKey() != null ? request.getIconKey() : settings.getIconKey());
        }
        InfoNoteSettings saved = repository.save(settings);
        log.info("Info note settings updated");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "infoNoteSettings", allEntries = true)
    public InfoNoteSettingsResponse uploadIcon(MultipartFile file) {
        InfoNoteSettings settings = getOrCreate();
        try {
            String url = fileStorageService.uploadFile(file, "info-note-icons");
            settings.setIconType("CUSTOM");
            settings.setIconUrl(url);
            InfoNoteSettings saved = repository.save(settings);
            log.info("Info note icon uploaded: {}", url);
            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Failed to upload info note icon", e);
            throw new RuntimeException("Failed to upload icon: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public int applyNoteToProductsWithoutCustomNote() {
        InfoNoteSettings settings = getOrCreate();
        int updated = productRepository.applyInfoNoteToProductsWithoutCustomNote(settings.getNote());
        log.info("Info note applied to {} products without a custom note", updated);
        return updated;
    }

    @Override
    @Transactional
    public int overwriteNoteOnAllProducts() {
        InfoNoteSettings settings = getOrCreate();
        int updated = productRepository.overwriteInfoNoteOnAllProducts(settings.getNote());
        log.info("Info note force-overwritten on {} products (including custom ones)", updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsWithCustomNote() {
        return productRepository.countProductsWithCustomInfoNote();
    }

    private InfoNoteSettingsResponse mapToResponse(InfoNoteSettings s) {
        return InfoNoteSettingsResponse.builder()
                .note(s.getNote())
                .iconType(s.getIconType())
                .iconKey(s.getIconKey())
                .iconUrl(s.getIconUrl())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}