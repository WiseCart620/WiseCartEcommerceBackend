package com.wisecartecommerce.ecommerce.Dto.Request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoNoteRequest {

    @NotBlank(message = "Note text is required")
    @Size(max = 500)
    private String note;

    private String iconType;
    private String iconKey;
    private String iconUrl;

    private boolean appliesToAll;
    private boolean active;
    private Integer displayOrder;

    private List<Long> productIds;
}