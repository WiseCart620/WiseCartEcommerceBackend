package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_banners")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    private String link;

    @Column(length = 20)
    private String bgColor = "#1e3a8a";

    @Column(length = 20)
    private String textColor = "#ffffff";

    private Boolean active = true;

    private Integer displayOrder = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist  void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   void onUpdate() { updatedAt = LocalDateTime.now(); }
}