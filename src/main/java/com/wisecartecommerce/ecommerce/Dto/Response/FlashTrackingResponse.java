package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashTrackingResponse {

    private String pno;
    private String origPno;
    private String returnedPno;
    private Integer state;
    private String stateText;
    private Long stateChangeAt;
    private List<RouteEntry> routes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteEntry {
        private Long routedAt;
        private String routeAction;
        private String message;
        private Integer state;
    }
}