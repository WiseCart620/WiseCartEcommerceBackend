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
import lombok.Data;

@Entity
@Table(name = "jnt_shipping_rates")
@Data
public class JntShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originProvince;

    @Column(nullable = false)
    private String originCity;

    @Column(nullable = false)
    private String destinationProvince;

    @Column(nullable = false)
    private String destinationCity;

    @Column(name = "destination_barangay")
    private String destinationBarangay;

    @Column(nullable = false)
    private String serviceType; // e.g. "EZ", "Standard"

    @Column(nullable = false)
    private String bagSize; // e.g. "Small (<=3KG)", "Medium (<=10KG)", "Large"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minWeightKg;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxWeightKg; // null = top/open-ended bracket for this route

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal itemAdditionalFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal additionalFeePerKgOverMax = BigDecimal.ZERO; // used past this bracket's max

    @Column(nullable = false)
    private Boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
