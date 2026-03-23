package com.wisecartecommerce.ecommerce.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.HomepageSectionRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.HomepageSectionResponse;

public interface HomepageSectionService {

    List<HomepageSectionResponse> getAllSections();

    List<HomepageSectionResponse> getActiveSections();

    HomepageSectionResponse getSectionByKey(String sectionKey);

    HomepageSectionResponse updateSection(String sectionKey, HomepageSectionRequest request);

    HomepageSectionResponse createSection(HomepageSectionRequest request);

    void deleteSection(String sectionKey);

    String uploadBannerImage(String sectionKey, MultipartFile file);

    void initializeDefaultSections();
}
