package com.wisecartecommerce.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vat_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal vatRate;

    @Column(name = "free_shipping_threshold", precision = 10, scale = 2, nullable = false)
    private BigDecimal freeShippingThreshold;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "store_email")
    private String storeEmail;

    @Column(name = "store_phone")
    private String storePhone;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cart_enabled", nullable = false)
    @Builder.Default
    private boolean cartEnabled = true;

    @Column(name = "buy_now_enabled", nullable = false)
    @Builder.Default
    private boolean buyNowEnabled = true;

    @Column(name = "jnt_origin_province")
    private String jntOriginProvince;

    @Column(name = "jnt_origin_city")
    private String jntOriginCity;

    @Column(name = "flash_enabled", nullable = false)
    @Builder.Default
    private boolean flashEnabled = true;

    @Column(name = "jnt_enabled", nullable = false)
    @Builder.Default
    private boolean jntEnabled = true;

    @Column(name = "default_weight_grams", nullable = false)
    @Builder.Default
    private int defaultWeightGrams = 500;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
