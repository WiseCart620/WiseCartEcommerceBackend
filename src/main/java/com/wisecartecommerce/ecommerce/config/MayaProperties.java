package com.wisecartecommerce.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "maya")
@Data
public class MayaProperties {
    private String publicKey;
    private String secretKey;
    private String baseUrl;
    private String successUrl;
    private String failureUrl;
    private String cancelUrl;
    private String webhookUrl;
}