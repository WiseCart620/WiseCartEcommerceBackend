package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RefundRequest {
    private String reason;
    private BigDecimal amount;
}