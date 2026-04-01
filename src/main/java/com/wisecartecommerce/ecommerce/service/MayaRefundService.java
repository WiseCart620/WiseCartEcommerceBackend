package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaRefundService {

    @Value("${maya.secret-key}")
    private String mayaSecretKey;

    @Value("${maya.base-url}")
    private String mayaBaseUrl;

    private final RestTemplate restTemplate;

    /**
     * Void a transaction using P3 API (same-day cancellation) Endpoint: POST
     * /p3/void
     *
     * @param transactionReferenceNo The transaction reference number from the
     * original payment
     * @param reason The reason for the void
     */
    public Map<String, Object> voidPayment(String transactionReferenceNo, String reason) {
        String url = mayaBaseUrl + "/p3/void";

        HttpHeaders headers = buildHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("transactionReferenceNo", transactionReferenceNo);
        body.put("reason", reason != null ? reason : "Customer requested cancellation");
        body.put("reasonCode", "00"); // 00 = customer requested

        log.info("Maya void request: url={} transactionRef={}", url, transactionReferenceNo);
        log.debug("Maya void request body: {}", body);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            log.info("Maya void successful: transactionRef={} response={}", transactionReferenceNo, response);
            return response;
        } catch (HttpClientErrorException e) {
            log.error("Maya void HTTP error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Maya void failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Maya void failed: transactionRef={} error={}", transactionReferenceNo, e.getMessage(), e);
            throw new RuntimeException("Maya void failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refund a completed transaction using P3 API (next day or later) Endpoint:
     * POST /p3/refund
     *
     * @param transactionReferenceNo The transaction reference number from the
     * original payment
     * @param amount The amount to refund (can be partial)
     * @param reason The reason for the refund
     */
    public Map<String, Object> refund(String transactionReferenceNo, BigDecimal amount, String reason) {
        String url = mayaBaseUrl + "/p3/refund";

        HttpHeaders headers = buildHeaders();

        Map<String, Object> amountObj = new HashMap<>();
        amountObj.put("value", amount);
        amountObj.put("currency", "PHP");

        Map<String, Object> body = new HashMap<>();
        body.put("transactionReferenceNo", transactionReferenceNo);
        body.put("amount", amountObj);
        body.put("reason", reason != null ? reason : "Customer requested refund");
        body.put("reasonCode", "00"); // 00 = customer requested

        log.info("Maya refund request: url={} transactionRef={} amount={}", url, transactionReferenceNo, amount);
        log.debug("Maya refund request body: {}", body);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            log.info("Maya refund successful: transactionRef={} response={}", transactionReferenceNo, response);
            return response;
        } catch (HttpClientErrorException e) {
            log.error("Maya refund HTTP error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Maya refund failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Maya refund failed: transactionRef={} error={}", transactionReferenceNo, e.getMessage(), e);
            throw new RuntimeException("Maya refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment details
     */
    public Map<String, Object> getPaymentDetails(String paymentId) {
        String url = mayaBaseUrl + "/payments/v1/payments/" + paymentId;

        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            ).getBody();
            log.info("Maya payment details retrieved: paymentId={}", paymentId);
            return response;
        } catch (Exception e) {
            log.error("Failed to get payment details: paymentId={} error={}", paymentId, e.getMessage());
            throw new RuntimeException("Failed to get payment details: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Request-Reference-No", generateRequestReferenceNumber());

        String auth = mayaSecretKey + ":";
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }

    private String generateRequestReferenceNumber() {
        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
