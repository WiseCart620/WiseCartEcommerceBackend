package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashNotifyResponse {

    private Long ticketPickupId;
    private String staffInfoName;
    private String staffInfoPhone;
    private String timeoutAtText;
    private String ticketMessage;
    private String upCountryNote;
}