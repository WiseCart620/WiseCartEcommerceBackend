package com.wisecartecommerce.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flash.express")
public class FlashExpressProperties {
    private String mchId;
    private String secretKey;
    private String warehouseNo;
    private String srcProvinceName;
    private String srcCityName;
    private String srcPostalCode;
    private String srcDetailAddress;
    private String srcName;
    private String srcPhone;
    private String baseUrl = "https://open-api.flashexpress.ph";

    public String createOrderUrl() {
        return baseUrl + "/open/v3/orders";
    }

    public String queryOrderUrl(String pno) {
        return baseUrl + "/open/v1/orders/" + pno + "/routes";
    }

    public String cancelOrderUrl(String pno) {
        return baseUrl + "/open/v1/orders/" + pno + "/cancel";
    }

    public String printLabelUrl(String pno) {
        return baseUrl + "/open/v1/orders/" + pno + "/pre_print";
    }

    public String deliveredInfoUrl(String pno) {
        return baseUrl + "/open/v1/orders/" + pno + "/deliveredInfo";
    }

    public String estimateRateUrl() {
        return baseUrl + "/open/v1/orders/estimate_rate";
    }

    public String warehousesUrl() {
        return baseUrl + "/open/v1/warehouses";
    }

    public String notifyUrl() {
        return baseUrl + "/open/v1/notify";
    }

    public String cancelNotifyUrl(Integer id) {
        return baseUrl + "/open/v1/notify/" + id + "/cancel";
    }

    public String notificationsUrl() {
        return baseUrl + "/open/v1/notifications";
    }
}