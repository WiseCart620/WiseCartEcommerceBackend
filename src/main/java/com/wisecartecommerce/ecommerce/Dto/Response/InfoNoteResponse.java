package com.wisecartecommerce.ecommerce.Dto.Response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoNoteResponse {
    private Long id;
    private String note;
    private String iconType;
    private String iconKey;
    private String iconUrl;
    private boolean appliesToAll;
    private boolean active;
    private Integer displayOrder;
    private List<Long> productIds;
    private LocalDateTime updatedAt;
}