package com.wisecartecommerce.ecommerce.Dto.Response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoNoteSettingsResponse {
    private String note;
    private String iconType;
    private String iconKey;
    private String iconUrl;
    private LocalDateTime updatedAt;
}