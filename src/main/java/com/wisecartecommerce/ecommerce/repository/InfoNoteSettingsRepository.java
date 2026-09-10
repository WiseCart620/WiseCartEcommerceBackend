package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.InfoNoteSettings;

@Repository
public interface InfoNoteSettingsRepository extends JpaRepository<InfoNoteSettings, Long> {
}