package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    private String description;

    @Column(name = "button_text")
    private String buttonText;

    @Column(name = "link_url")
    private String link;

    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Tailwind gradient class string, e.g. "from-orange-500 to-red-500"
     * Stored as-is and passed directly to the frontend.
     */
    private String color;

    @Column(name = "display_order")
    private Integer displayOrder;

    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "overlay_opacity")
    private Integer overlayOpacity;
}