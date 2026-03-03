package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "testimonials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(length = 100)
    private String customerTitle;   // e.g. "Verified Buyer", "Fashion Blogger"

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String review;

    /** 1–5 */
    @Column(nullable = false)
    @Builder.Default
    private Integer rating = 5;

    @Column(name = "product_name", length = 150)
    private String productName;     // optional — which product they reviewed

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}