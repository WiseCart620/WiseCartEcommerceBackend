package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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

    // Original method - keep this for backward compatibility
    public Map<String, String> createCheckout(String checkoutRef, BigDecimal amount,
            String firstName, String lastName, String phone, String email) {
        return createCheckoutWithRedirects(
                checkoutRef, amount, firstName, lastName, phone, email,
                null, null, null
        );
    }

    // Overloaded method for backward compatibility
    public Map<String, String> createCheckout(String checkoutRef, BigDecimal amount) {
        return createCheckout(checkoutRef, amount, null, null, null, null);
    }

    // ✅ NEW METHOD with custom redirect URLs
    public Map<String, String> createCheckoutWithRedirects(
            String checkoutRef,
            BigDecimal amount,
            String firstName,
            String lastName,
            String phone,
            String email,
            String customSuccessUrl,
            String customFailureUrl,
            String customCancelUrl) {

        log.info("Creating Maya checkout: ref={} amount={}", checkoutRef, amount);

        // Use default URLs if custom ones are not provided
        String successUrl = customSuccessUrl != null ? customSuccessUrl : props.getSuccessUrl() + "?ref=" + checkoutRef;
        String failureUrl = customFailureUrl != null ? customFailureUrl : props.getFailureUrl() + "?ref=" + checkoutRef;
        String cancelUrl = customCancelUrl != null ? customCancelUrl : props.getCancelUrl() + "?ref=" + checkoutRef;

        // ✅ Use PUBLIC key for creating checkouts
        String credentials = Base64.getEncoder()
                .encodeToString((props.getPublicKey() + ":").getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // ✅ REQUIRED header
        headers.set("X-REQUEST-VIA", "API");

        if (amount == null || amount.doubleValue() <= 0) {
            throw new RuntimeException("Invalid checkout amount: " + amount);
        }
        if (checkoutRef == null || checkoutRef.length() > 36) {
            throw new RuntimeException("Invalid checkout ref: " + checkoutRef);
        }

        // ✅ Format phone number to international format
        String formattedPhone = formatPhoneNumber(phone);
        String formattedEmail = (email != null && !email.isEmpty()) ? email : "customer@example.com";
        String formattedFirstName = (firstName != null && !firstName.isEmpty()) ? firstName : "Customer";
        String formattedLastName = (lastName != null && !lastName.isEmpty()) ? lastName : "User";

        // ✅ Build buyer object (REQUIRED for Maya Wallet)
        Map<String, Object> buyer = new HashMap<>();
        buyer.put("firstName", formattedFirstName);
        buyer.put("lastName", formattedLastName);
        buyer.put("contact", Map.of(
                "phone", formattedPhone,
                "email", formattedEmail
        ));

        // ✅ Add shipping address (REQUIRED for Maya Wallet)
        Map<String, String> shippingAddress = new HashMap<>();
        shippingAddress.put("line1", "Test Address Line 1");
        shippingAddress.put("line2", "Test Address Line 2");
        shippingAddress.put("city", "Manila");
        shippingAddress.put("state", "Metro Manila");
        shippingAddress.put("zipCode", "1000");
        shippingAddress.put("country", "PH");

        buyer.put("shippingAddress", shippingAddress);
        buyer.put("billingAddress", shippingAddress);

        // ✅ Build complete request body
        Map<String, Object> body = new HashMap<>();
        body.put("totalAmount", Map.of("value", amount, "currency", "PHP"));
        body.put("requestReferenceNumber", checkoutRef);
        body.put("buyer", buyer);

        // ✅ Add items array (REQUIRED)
        body.put("items", List.of(Map.of(
                "name", "WiseCart Order",
                "quantity", 1,
                "totalAmount", Map.of("value", amount, "currency", "PHP")
        )));

        // ✅ Use custom redirect URLs
        body.put("redirectUrl", Map.of(
                "success", successUrl,
                "failure", failureUrl,
                "cancel", cancelUrl
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    props.getBaseUrl() + "/checkout/v1/checkouts", request, Map.class);

            log.info("Maya API Response: {}", response);

            String redirectUrl = null;
            String checkoutId = null;

            if (response != null) {
                redirectUrl = (String) response.get("redirectUrl");
                checkoutId = (String) response.get("checkoutId");
                if (checkoutId == null && response.containsKey("id")) {
                    checkoutId = (String) response.get("id");
                }
            }

            if (redirectUrl == null) {
                log.error("Maya response missing redirectUrl: {}", response);
                throw new RuntimeException("Maya checkout response missing redirect URL");
            }

            log.info("Maya checkout created: ref={} checkoutId={}", checkoutRef, checkoutId);
            return Map.of("redirectUrl", redirectUrl, "checkoutId", checkoutId != null ? checkoutId : "");

        } catch (HttpClientErrorException e) {
            log.error("Maya client error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());

            // Parse error for better debugging
            try {
                Map<String, Object> errorBody = e.getResponseBodyAs(Map.class);
                log.error("Maya error details: {}", errorBody);
            } catch (Exception ex) {
                // Ignore
            }

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
            // ✅ Use SECRET key for status checks
            String credentials = Base64.getEncoder()
                    .encodeToString((props.getSecretKey() + ":").getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + credentials);
            headers.set("X-REQUEST-VIA", "API");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
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
            // ✅ Use SECRET key for details
            String credentials = Base64.getEncoder()
                    .encodeToString((props.getSecretKey() + ":").getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + credentials);
            headers.set("X-REQUEST-VIA", "API");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    props.getBaseUrl() + "/checkout/v1/checkouts/" + mayaCheckoutId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            ).getBody();

            if (response != null) {
                log.info("Maya checkout details retrieved for checkoutId={}", mayaCheckoutId);
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to get Maya checkout details for checkoutId={}: {}", mayaCheckoutId, e.getMessage(), e);
        }
        return null;
    }

    // ✅ Helper method to format phone number to international format
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "639171234567"; // Default test number
        }

        // Remove all non-digit characters
        String cleaned = phone.replaceAll("[^0-9]", "");

        // Format to international format
        if (cleaned.length() == 10) {
            // Local format: 9171234567 -> 639171234567
            return "63" + cleaned;
        } else if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            // Local format with leading 0: 09171234567 -> 639171234567
            return "63" + cleaned.substring(1);
        } else if (cleaned.length() == 12 && cleaned.startsWith("63")) {
            // Already international format
            return cleaned;
        } else {
            return "639171234567";
        }
    }
}
