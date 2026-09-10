package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoNoteSettingsRequest {

    @Size(max = 500)
    private String note;

    private String iconType;
    private String iconKey;
    private String iconUrl;
}