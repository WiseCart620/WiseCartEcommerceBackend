package com.wisecartecommerce.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flash.express")
public class FlashExpressProperties {

    /** Merchant ID from Flash Express portal e.g. AA0007 */
    private String mchId;

    /** Secret API key from Flash Express support */
    private String secretKey;

    /** Warehouse number (optional - used as default shipper) */
    private String warehouseNo;

    // ── Shipper (src) address for rate estimation ──
    /** Your store's province name e.g. "Metro Manila" */
    private String srcProvinceName;
    /** Your store's city name e.g. "Taguig City" */
    private String srcCityName;
    /** Your store's postal code e.g. "1634" */
    private String srcPostalCode;

    /** prod: https://open-api.flashexpress.ph
     *  sandbox: https://open-api-tra.flashexpress.ph */
    private String baseUrl = "https://open-api.flashexpress.ph";

    // ── Derived URL helpers ──

    public String createOrderUrl()         { return baseUrl + "/open/v3/orders"; }
    public String queryOrderUrl(String pno){ return baseUrl + "/open/v1/orders/" + pno + "/routes"; }
    public String cancelOrderUrl(String pno){ return baseUrl + "/open/v1/orders/" + pno + "/cancel"; }
    public String printLabelUrl(String pno){ return baseUrl + "/open/v1/orders/" + pno + "/pre_print"; }
    public String deliveredInfoUrl(String pno){ return baseUrl + "/open/v1/orders/" + pno + "/deliveredInfo"; }
    public String estimateRateUrl()        { return baseUrl + "/open/v1/orders/estimate_rate"; }
    public String warehousesUrl()          { return baseUrl + "/open/v1/warehouses"; }
    public String notifyUrl()              { return baseUrl + "/open/v1/notify"; }
    public String cancelNotifyUrl(Integer id){ return baseUrl + "/open/v1/notify/" + id + "/cancel"; }
    public String notificationsUrl()       { return baseUrl + "/open/v1/notifications"; }
}