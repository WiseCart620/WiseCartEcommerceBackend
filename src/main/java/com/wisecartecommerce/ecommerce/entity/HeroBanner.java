package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "hero_banners")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HeroBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String badge;

    @Column(length = 200)
    private String subtitle;

    @Column(name = "button_text", length = 50)
    @Builder.Default
    private String buttonText = "Shop Now";

    @Column(name = "button_link", length = 255)
    @Builder.Default
    private String buttonLink = "/products";

    @Column(name = "image_url")
    private String imageUrl;

    /** light or dark — controls text color on top of the image */
    @Column(name = "text_color", length = 10)
    @Builder.Default
    private String textColor = "light";

    /** 0–100 — overlay darkness percentage */
    @Column(name = "overlay_opacity")
    @Builder.Default
    private Integer overlayOpacity = 40;

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