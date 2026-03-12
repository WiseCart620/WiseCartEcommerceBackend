package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {}