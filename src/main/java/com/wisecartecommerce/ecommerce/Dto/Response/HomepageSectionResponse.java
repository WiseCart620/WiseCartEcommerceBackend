package com.wisecartecommerce.ecommerce.Dto.Response;

import java.time.LocalDateTime;
import java.util.List;

import com.wisecartecommerce.ecommerce.enums.SectionMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomepageSectionResponse {

    private Long id;
    private String sectionKey;
    private String title;
    private String subtitle;
    private SectionMode mode;
    private Long categoryId;
    private String categoryName;
    private Integer limit;
    private boolean active;
    private Integer displayOrder;

    private boolean showTimer;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private java.time.LocalDateTime timerEndsAt;

    private String timerLabel;

    /**
     * Milliseconds remaining until timerEndsAt (computed, for convenience)
     */
    private Long timerRemainingMs;

    /**
     * Populated for MANUAL and CATEGORY modes — the actual products to display
     */
    private List<ProductResponse> products;

    /**
     * IDs of manually selected products (admin use)
     */
    private List<Long> productIds;

    private LocalDateTime updatedAt;

    private String sectionBannerUrl;
    private String sectionBannerLink;

    private String sectionBannerTitle;
    private String sectionBannerDescription;
    private Integer sectionBannerOverlay;

}
