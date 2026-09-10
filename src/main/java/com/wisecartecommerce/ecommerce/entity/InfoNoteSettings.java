package com.wisecartecommerce.ecommerce.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "info_note_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoNoteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note", length = 500)
    private String note;

    /** PRESET or CUSTOM */
    @Column(name = "icon_type", length = 20)
    @Builder.Default
    private String iconType = "PRESET";

    /** e.g. "FiTruck" — only used when iconType = PRESET */
    @Column(name = "icon_key", length = 50)
    @Builder.Default
    private String iconKey = "FiTruck";

    /** uploaded image URL — only used when iconType = CUSTOM */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}