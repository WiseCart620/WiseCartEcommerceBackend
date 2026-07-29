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
@Table(name = "jnt_shipping_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column
    private String destinationCity;

    @Column(name = "destination_barangay")
    private String destinationBarangay;

    @Column(nullable = false)
    private String serviceType;

    @Column(nullable = false)
    private String bagSize;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal itemAdditionalFee = BigDecimal.ZERO;

    @Column(name = "overweight_additional_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal overweightAdditionalFee = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
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
