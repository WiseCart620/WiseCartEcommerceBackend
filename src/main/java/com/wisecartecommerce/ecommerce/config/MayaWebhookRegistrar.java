package com.wisecartecommerce.ecommerce.config;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MayaWebhookRegistrar implements ApplicationRunner {

    private final MayaProperties props;
    private final RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (props.getWebhookUrl() == null || props.getWebhookUrl().isBlank()) {
            log.warn("Maya webhook URL not configured, skipping registration.");
            return;
        }

        String credentials = Base64.getEncoder()
                .encodeToString((props.getSecretKey() + ":").getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> events = List.of(
                "PAYMENT_SUCCESS", "PAYMENT_FAILED",
                "PAYMENT_EXPIRED", "PAYMENT_CANCELLED"
        );

        for (String event : events) {
            try {
                Map<String, String> body = Map.of(
                        "name", event,
                        "callbackUrl", props.getWebhookUrl()
                );
                restTemplate.postForObject(
                        props.getBaseUrl() + "/payments/v1/webhooks",
                        new HttpEntity<>(body, headers),
                        Map.class
                );
                log.info("✅ Registered Maya webhook: {}", event);
            } catch (HttpClientErrorException e) {
                // PY0039 means webhook already exists — that's fine
                if (e.getStatusCode().value() == 400
                        && e.getResponseBodyAsString().contains("PY0039")) {
                    log.info("Maya webhook already registered: {}", event);
                } else {
                    log.warn("Maya webhook registration failed for {}: {} - {}",
                            event, e.getStatusCode(), e.getResponseBodyAsString());
                }
            } catch (Exception e) {
                log.warn("Maya webhook registration skipped for {}: {}", event, e.getMessage());
            }
        }
    }
}
