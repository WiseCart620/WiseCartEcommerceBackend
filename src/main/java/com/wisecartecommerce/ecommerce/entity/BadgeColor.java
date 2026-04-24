package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge_colors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeColor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String badgeName;
    
    @Column(nullable = false)
    private String colorClass;
    
    @Column(nullable = false)
    private boolean active;
    
    @Column(nullable = false)
    private int displayOrder;
}