package com.wisecartecommerce.ecommerce.service;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.wisecartecommerce.ecommerce.config.MayaProperties;
import com.wisecartecommerce.ecommerce.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaService {

    private final MayaProperties props;
    private final RestTemplate restTemplate;

    public String createCheckout(Order order) {
        // FIX: Use PUBLIC KEY for checkout creation, not secret key!
        log.info("Maya publicKey being used: {}", 
                props.getPublicKey() != null ? props.getPublicKey().substring(0, 10) + "..." : "NULL");
        log.info("Maya baseUrl: {}", props.getBaseUrl());

        // Use PUBLIC key for authentication
        String credentials = Base64.getEncoder()
                .encodeToString((props.getPublicKey() + ":").getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Validate order amount - ensure it's positive
        if (order.getFinalAmount() == null || order.getFinalAmount().doubleValue() <= 0) {
            throw new RuntimeException("Invalid order amount: " + order.getFinalAmount());
        }

        Map<String, Object> body = Map.of(
                "totalAmount", Map.of(
                        "value", order.getFinalAmount().doubleValue(),
                        "currency", "PHP"
                ),
                "requestReferenceNumber", "WC-" + order.getId(),
                "redirectUrl", Map.of(
                        "success", props.getSuccessUrl() + "?orderId=" + order.getId(),
                        "failure", props.getFailureUrl() + "?orderId=" + order.getId(),
                        "cancel", props.getCancelUrl() + "?orderId=" + order.getId()
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.debug("Maya request URL: {}", props.getBaseUrl() + "/checkout/v1/checkouts");
            log.debug("Maya request body: {}", body);
            
            Map response = restTemplate.postForObject(
                    props.getBaseUrl() + "/checkout/v1/checkouts",
                    request,
                    Map.class
            );
            
            // Maya API returns redirectUrl that the frontend should redirect to
            String redirectUrl = (String) response.get("redirectUrl");
            
            if (redirectUrl == null) {
                log.error("Maya response missing redirectUrl: {}", response);
                throw new RuntimeException("Maya checkout response missing redirect URL");
            }
            
            log.info("Maya checkout created for order {}: {}", order.getOrderNumber(), redirectUrl);
            return redirectUrl;
            
        } catch (HttpClientErrorException e) {
            log.error("Maya API client error for order {}: status={} body={}",
                    order.getOrderNumber(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            // Check for specific error codes
            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("Maya authentication failed. Please check your API keys.");
            } else if (e.getStatusCode().value() == 400) {
                throw new RuntimeException("Invalid request to Maya: " + e.getResponseBodyAsString());
            } else {
                throw new RuntimeException("Maya checkout failed: " + e.getStatusCode());
            }
            
        } catch (HttpServerErrorException e) {
            log.error("Maya API server error for order {}: status={} body={}",
                    order.getOrderNumber(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Maya service temporarily unavailable. Please try again later.");
            
        } catch (RestClientException e) {
            log.error("Maya checkout connection error for order {}: {}",
                    order.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Maya payment unavailable. Please try another method.");
        }
    }
}