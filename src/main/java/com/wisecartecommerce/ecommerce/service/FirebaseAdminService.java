package com.wisecartecommerce.ecommerce.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class FirebaseAdminService {

    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String FIREBASE_AUTH_URL
            = "https://identitytoolkit.googleapis.com/v1/accounts";

    public void deleteUser(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            log.debug("Skipping Firebase sync — no firebaseUid");
            return;
        }
        try {
            HttpEntity<Map<String, Object>> request = buildRequest(
                    Map.of("localId", firebaseUid, "disableUser", true)
            );
            restTemplate.postForEntity(
                    FIREBASE_AUTH_URL + ":update?key=" + firebaseApiKey,
                    request,
                    String.class
            );
            log.info("Firebase user disabled (deleted from app): {}", firebaseUid);
        } catch (HttpClientErrorException e) {
            log.warn("Firebase disable failed for uid {}: {} - {}",
                    firebaseUid, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Firebase disable error for uid {}: {}", firebaseUid, e.getMessage());
        }
    }

    public void disableUser(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            log.debug("Skipping Firebase disable — no firebaseUid");
            return;
        }
        try {
            restTemplate.postForEntity(
                    FIREBASE_AUTH_URL + ":update?key=" + firebaseApiKey,
                    buildRequest(Map.of("localId", firebaseUid, "disableUser", true)),
                    String.class
            );
            log.info("Firebase user disabled: {}", firebaseUid);
        } catch (HttpClientErrorException e) {
            log.warn("Firebase disable failed for uid {}: {} - {}",
                    firebaseUid, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Firebase disable error for uid {}: {}", firebaseUid, e.getMessage());
        }
    }

    public void enableUser(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            log.debug("Skipping Firebase enable — no firebaseUid");
            return;
        }
        try {
            restTemplate.postForEntity(
                    FIREBASE_AUTH_URL + ":update?key=" + firebaseApiKey,
                    buildRequest(Map.of("localId", firebaseUid, "disableUser", false)),
                    String.class
            );
            log.info("Firebase user enabled: {}", firebaseUid);
        } catch (HttpClientErrorException e) {
            log.warn("Firebase enable failed for uid {}: {} - {}",
                    firebaseUid, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Firebase enable error for uid {}: {}", firebaseUid, e.getMessage());
        }
    }

    private <T> HttpEntity<T> buildRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
