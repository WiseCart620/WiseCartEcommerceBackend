package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.entity.AppSettings;
import com.wisecartecommerce.ecommerce.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping
    public ResponseEntity<AppSettings> getSettings() {
        return ResponseEntity.ok(getOrCreateSettings());
    }

    @PutMapping
    public ResponseEntity<AppSettings> updateSettings(@RequestBody AppSettings request) {
        AppSettings settings = getOrCreateSettings();
        settings.setVatRate(request.getVatRate());
        settings.setFreeShippingThreshold(request.getFreeShippingThreshold());
        settings.setStoreName(request.getStoreName());
        settings.setStoreEmail(request.getStoreEmail());
        settings.setStorePhone(request.getStorePhone());
        return ResponseEntity.ok(repository.save(settings));
    }
}