package com.wisecartecommerce.ecommerce.controller.admin;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.entity.AppSettings;
import com.wisecartecommerce.ecommerce.repository.AppSettingsRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AppSettingsController {

    private final AppSettingsRepository repository;

    private AppSettings getOrCreateSettings() {
        return repository.findAll().stream()
                .findFirst()
                .orElseGet(this::createDefaultSettings);
    }

    private AppSettings createDefaultSettings() {
        AppSettings defaults = new AppSettings();
        defaults.setVatRate(new BigDecimal("0.12"));
        defaults.setFreeShippingThreshold(new BigDecimal("599"));
        defaults.setStoreName("WiseCart");
        defaults.setStoreEmail("");
        defaults.setStorePhone("");
        return repository.save(defaults);
    }

    @GetMapping("/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppSettings> getSettings() {
        return ResponseEntity.ok(getOrCreateSettings());
    }


    @GetMapping("/public/storefront/settings")
    public ResponseEntity<?> getPublicSettings() {
        AppSettings s = getOrCreateSettings();
        return ResponseEntity.ok(java.util.Map.of(
                "cartEnabled", s.isCartEnabled(),
                "buyNowEnabled", s.isBuyNowEnabled(),
                "vatRate", s.getVatRate(),
                "freeShippingThreshold", s.getFreeShippingThreshold()
        ));
    }

    
    @PutMapping("/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppSettings> updateSettings(@RequestBody AppSettings request) {
        AppSettings settings = getOrCreateSettings();
        settings.setVatRate(request.getVatRate());
        settings.setFreeShippingThreshold(request.getFreeShippingThreshold());
        settings.setStoreName(request.getStoreName());
        settings.setStoreEmail(request.getStoreEmail());
        settings.setStorePhone(request.getStorePhone());
        settings.setCartEnabled(request.isCartEnabled());
        settings.setBuyNowEnabled(request.isBuyNowEnabled());
        return ResponseEntity.ok(repository.save(settings));
    }
}
