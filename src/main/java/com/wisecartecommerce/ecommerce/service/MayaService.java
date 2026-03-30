package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaService {

    private final MayaProperties props;
    private final RestTemplate restTemplate;

    public Map<String, String> createCheckout(String checkoutRef, BigDecimal amount) {
        log.info("Creating Maya checkout: ref={} amount={}", checkoutRef, amount);

        String credentials = Base64.getEncoder()
                .encodeToString((props.getPublicKey() + ":").getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (amount == null || amount.doubleValue() <= 0) {
            throw new RuntimeException("Invalid checkout amount: " + amount);
        }
        if (checkoutRef == null || checkoutRef.length() > 36) {
            throw new RuntimeException("Invalid checkout ref: " + checkoutRef);
        }

        Map<String, Object> body = Map.of(
                "totalAmount", Map.of("value", amount.doubleValue(), "currency", "PHP"),
                "requestReferenceNumber", checkoutRef,
                "redirectUrl", Map.of(
                        "success", props.getSuccessUrl() + "?ref=" + checkoutRef,
                        "failure", props.getFailureUrl() + "?ref=" + checkoutRef,
                        "cancel", props.getCancelUrl() + "?ref=" + checkoutRef
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(
                    props.getBaseUrl() + "/checkout/v1/checkouts", request, Map.class);

            String redirectUrl = (String) response.get("redirectUrl");
            String checkoutId = (String) response.get("checkoutId"); // ← ADD THIS

            if (redirectUrl == null) {
                log.error("Maya response missing redirectUrl: {}", response);
                throw new RuntimeException("Maya checkout response missing redirect URL");
            }

            log.info("Maya checkout created: ref={} checkoutId={}", checkoutRef, checkoutId);
            return Map.of("redirectUrl", redirectUrl, "checkoutId", checkoutId != null ? checkoutId : "");

        } catch (HttpClientErrorException e) {
            log.error("Maya client error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("Maya authentication failed. Check your API keys.");
            }
            if (e.getStatusCode().value() == 400) {
                throw new RuntimeException("Invalid Maya request: " + e.getResponseBodyAsString());
            }
            throw new RuntimeException("Maya checkout failed: " + e.getStatusCode());

        } catch (HttpServerErrorException e) {
            log.error("Maya server error: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Maya service temporarily unavailable.");

        } catch (RestClientException e) {
            log.error("Maya connection error: {}", e.getMessage());
            throw new RuntimeException("Maya payment unavailable. Please try another method.");
        }
    }

    public String getCheckoutStatus(String mayaCheckoutId) {
        if (mayaCheckoutId == null || mayaCheckoutId.isBlank()) {
            log.warn("Cannot check Maya status: checkoutId is null");
            return null;
        }

        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((props.getSecretKey() + ":").getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + credentials);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            Map response = restTemplate.exchange(
                    props.getBaseUrl() + "/checkout/v1/checkouts/" + mayaCheckoutId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            ).getBody();

            if (response != null) {
                String status = (String) response.get("status");
                log.info("Maya checkout status for checkoutId={}: {}", mayaCheckoutId, status);
                return status;
            }
        } catch (Exception e) {
            log.warn("Failed to get Maya checkout status for checkoutId={}: {}", mayaCheckoutId, e.getMessage());
        }
        return null;
    }

    public Map<String, Object> getCheckoutDetails(String mayaCheckoutId) {
        if (mayaCheckoutId == null || mayaCheckoutId.isBlank()) {
            log.warn("Cannot get Maya checkout details: checkoutId is null");
            return null;
        }

        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((props.getSecretKey() + ":").getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + credentials);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            Map response = restTemplate.exchange(
                    props.getBaseUrl() + "/checkout/v1/checkouts/" + mayaCheckoutId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            ).getBody();

            if (response != null) {
                // Log the FULL response at WARN level so you can see it clearly
                log.warn("=== MAYA API FULL RESPONSE for checkoutId={} ===: {}", mayaCheckoutId, response);
                log.info("Maya checkout details retrieved for checkoutId={}", mayaCheckoutId);
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to get Maya checkout details for checkoutId={}: {}", mayaCheckoutId, e.getMessage(), e);
        }
        return null;
    }
}
