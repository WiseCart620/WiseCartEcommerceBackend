package com.wisecartecommerce.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_add_ons", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id",
        "add_on_product_id" }))
@Data
@NoArgsConstructor
public class ProductAddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_product_id")
    @JsonIgnore
    private Product addOnProduct;

    @Column(name = "special_price", precision = 10, scale = 2)
    private BigDecimal specialPrice;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
