package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashExpressSettingsRepository extends JpaRepository<FlashExpressSettings, Long> {
    // There will always be exactly one row (id=1)
}