package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;

@Repository
public interface FlashExpressSettingsRepository extends JpaRepository<FlashExpressSettings, Long> {
}