package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Request.HomepageSectionRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.HomepageSectionResponse;

import java.util.List;

public interface HomepageSectionService {
    List<HomepageSectionResponse> getAllSections();

    List<HomepageSectionResponse> getActiveSections();

    HomepageSectionResponse getSectionByKey(String sectionKey);

    HomepageSectionResponse updateSection(String sectionKey, HomepageSectionRequest request);

    HomepageSectionResponse createSection(HomepageSectionRequest request);

    void deleteSection(String sectionKey);

    void initializeDefaultSections();
}