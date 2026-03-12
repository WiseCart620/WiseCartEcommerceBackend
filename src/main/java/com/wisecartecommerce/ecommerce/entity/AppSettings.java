package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}