// ─── HomepageSectionRepository.java ─────────────────────────────────────────
package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.HomepageSectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomepageSectionRepository extends JpaRepository<HomepageSectionConfig, Long> {

    List<HomepageSectionConfig> findAllByOrderByDisplayOrderAsc();

    List<HomepageSectionConfig> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<HomepageSectionConfig> findBySectionKey(String sectionKey);
}
