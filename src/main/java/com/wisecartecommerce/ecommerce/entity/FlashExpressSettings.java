package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flash_express_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashExpressSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mch_id", nullable = false)
    private String mchId;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "warehouse_no")
    private String warehouseNo;

    @Column(name = "src_name", nullable = false)
    private String srcName;

    @Column(name = "src_phone", nullable = false)
    private String srcPhone;

    @Column(name = "src_province_name", nullable = false)
    private String srcProvinceName;

    @Column(name = "src_city_name", nullable = false)
    private String srcCityName;

    @Column(name = "src_postal_code", nullable = false)
    private String srcPostalCode;

    @Column(name = "src_detail_address", nullable = false, columnDefinition = "TEXT")
    private String srcDetailAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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