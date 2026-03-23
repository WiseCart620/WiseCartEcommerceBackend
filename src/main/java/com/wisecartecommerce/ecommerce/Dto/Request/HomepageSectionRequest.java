package com.wisecartecommerce.ecommerce.Dto.Request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wisecartecommerce.ecommerce.enums.SectionMode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomepageSectionRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100)
    private String title;

    private String sectionKey;

    private boolean showTimer;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private java.time.LocalDateTime timerEndsAt;

    @jakarta.validation.constraints.Size(max = 60)
    private String timerLabel;

    @Size(max = 200)
    private String subtitle;

    @NotNull(message = "Mode is required")
    private SectionMode mode;

    /**
     * Required when mode = CATEGORY
     */
    private Long categoryId;

    /**
     * Required when mode = MANUAL — ordered list of product IDs
     */
    private List<Long> productIds;

    @Min(1)
    @Max(20)
    private Integer limit;

    private boolean active;

    private Integer displayOrder;

    private String sectionBannerUrl;
    private String sectionBannerLink;

    private String sectionBannerTitle;
    private String sectionBannerDescription;
    private Integer sectionBannerOverlay;

}
