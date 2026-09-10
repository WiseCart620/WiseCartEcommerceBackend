package com.wisecartecommerce.ecommerce.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "info_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String note;

    @Column(name = "icon_type", length = 20)
    @Builder.Default
    private String iconType = "PRESET";

    @Column(name = "icon_key", length = 50)
    @Builder.Default
    private String iconKey = "FiTruck";

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "applies_to_all", nullable = false)
    @Builder.Default
    private boolean appliesToAll = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /** Only meaningful when appliesToAll = false */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "info_note_products", joinColumns = @JoinColumn(name = "info_note_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<Long> productIds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}