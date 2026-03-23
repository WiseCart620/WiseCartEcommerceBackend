package com.wisecartecommerce.ecommerce.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.wisecartecommerce.ecommerce.enums.SectionMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "homepage_section_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageSectionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique key identifying the section: FEATURED, NEW_ARRIVALS, BEST_SELLERS,
     * HOT_DEALS
     */
    @Column(nullable = false, unique = true, length = 50)
    private String sectionKey;

    /**
     * Display title shown on the storefront
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * Display subtitle shown below the title
     */
    @Column(length = 200)
    private String subtitle;

    /**
     * How to populate this section
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private SectionMode mode = SectionMode.AUTO;

    /**
     * Used when mode = CATEGORY
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Max number of products to display
     */
    @Column(name = "product_limit", nullable = false)
    @Builder.Default
    private Integer limit = 8;

    /**
     * Whether this section is visible on the storefront
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Display order on the homepage (lower = higher on page)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Manually selected products (used when mode = MANUAL). Ordered by
     * displayOrder in HomepageSectionProduct.
     */
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<HomepageSectionProduct> sectionProducts = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean showTimer = false;

    @Column
    private java.time.LocalDateTime timerEndsAt;

    @Column(length = 60)
    private String timerLabel;

    @Column(length = 500)
    @Builder.Default
    private String sectionBannerUrl = null;

    @Column(length = 500)
    @Builder.Default
    private String sectionBannerLink = null;

    @Column(length = 200)
    private String sectionBannerTitle;

    @Column(length = 500)
    private String sectionBannerDescription;

    @Column(name = "section_banner_overlay")
    @Builder.Default
    private Integer sectionBannerOverlay = 40;
}
