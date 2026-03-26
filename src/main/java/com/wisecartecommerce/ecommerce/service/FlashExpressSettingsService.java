package com.wisecartecommerce.ecommerce.service;

import org.springframework.stereotype.Service;

import com.wisecartecommerce.ecommerce.config.FlashExpressProperties;
import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import com.wisecartecommerce.ecommerce.repository.FlashExpressSettingsRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashExpressSettingsService {

    private final FlashExpressSettingsRepository repo;
    private final FlashExpressProperties yamlProps;


@Transactional
public FlashExpressSettings getSettings() {
    return repo.findById(1L).orElseGet(() -> {
        log.info("No Flash Express settings in DB — seeding from yaml properties");
        FlashExpressSettings seed = FlashExpressSettings.builder()
                .mchId(yamlProps.getMchId())
                .secretKey(yamlProps.getSecretKey())
                .baseUrl(yamlProps.getBaseUrl())
                .warehouseNo(yamlProps.getWarehouseNo())
                .srcName(yamlProps.getSrcName())
                .srcPhone(yamlProps.getSrcPhone())
                .srcProvinceName(yamlProps.getSrcProvinceName())
                .srcCityName(yamlProps.getSrcCityName())
                .srcPostalCode(yamlProps.getSrcPostalCode())
                .srcDetailAddress(yamlProps.getSrcDetailAddress())
                .build();
        return repo.save(seed);
    });
}

    /**
     * Updates the single settings row in DB.
     */
    @Transactional
    public FlashExpressSettings updateSettings(FlashExpressSettings updated) {
        FlashExpressSettings existing = getSettings();
        existing.setMchId(updated.getMchId());
        existing.setSecretKey(updated.getSecretKey());
        existing.setBaseUrl(updated.getBaseUrl());
        existing.setWarehouseNo(updated.getWarehouseNo());
        existing.setSrcName(updated.getSrcName());
        existing.setSrcPhone(updated.getSrcPhone());
        existing.setSrcProvinceName(updated.getSrcProvinceName());
        existing.setSrcCityName(updated.getSrcCityName());
        existing.setSrcPostalCode(updated.getSrcPostalCode());
        existing.setSrcDetailAddress(updated.getSrcDetailAddress());
        FlashExpressSettings saved = repo.save(existing);
        log.info("Flash Express settings updated by admin");
        return saved;
    }
}
