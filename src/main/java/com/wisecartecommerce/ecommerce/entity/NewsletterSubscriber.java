package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String source; // WEBSITE, FOOTER, etc.

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    public void onCreate() {
        if (subscribedAt == null) subscribedAt = LocalDateTime.now();
        if (source == null) source = "WEBSITE";
    }
}