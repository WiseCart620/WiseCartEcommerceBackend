package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.config.FlashExpressProperties;
import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import com.wisecartecommerce.ecommerce.repository.FlashExpressSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashExpressSettingsService {

    private final FlashExpressSettingsRepository repo;
    private final FlashExpressProperties yamlProps;

    /**
     * Returns current settings from DB (row id=1).
     * If no row exists yet, seeds from yaml and saves.
     */
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