package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.Data;

@Data
public class AnnouncementBannerDTO {
    private Long    id;
    private String  text;
    private String  link;
    private String  bgColor;
    private String  textColor;
    private Boolean active;
    private Integer displayOrder;
}