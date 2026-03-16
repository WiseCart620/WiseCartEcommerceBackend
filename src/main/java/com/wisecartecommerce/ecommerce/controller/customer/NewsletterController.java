package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.entity.NewsletterSubscriber;
import com.wisecartecommerce.ecommerce.repository.NewsletterSubscriberRepository;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterSubscriberRepository subscriberRepo;
    private final OrderRepository orderRepo;

    // ── Public: subscribe ─────────────────────────────────────────────────────

    @PostMapping("/public/newsletter/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email address"));
        }

        Optional<NewsletterSubscriber> existing = subscriberRepo.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().isActive()) {
                return ResponseEntity.ok(Map.of("message", "Already subscribed"));
            }
            // Re-subscribe
            existing.get().setActive(true);
            subscriberRepo.save(existing.get());
            return ResponseEntity.ok(Map.of("message", "Re-subscribed successfully"));
        }

        subscriberRepo.save(NewsletterSubscriber.builder()
            .email(email)
            .source(body.getOrDefault("source", "FOOTER"))
            .active(true)
            .build());

        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }

    // ── Admin: get all contacts (subscribers + order emails) ──────────────────

    @GetMapping("/admin/newsletter/contacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllContacts() {
        // Newsletter subscribers
        List<Map<String, Object>> subscribers = subscriberRepo
            .findByActiveTrueOrderBySubscribedAtDesc()
            .stream()
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("email", s.getEmail());
                m.put("source", s.getSource());
                m.put("subscribedAt", s.getSubscribedAt());
                m.put("type", "SUBSCRIBER");
                return m;
            })
            .collect(Collectors.toList());

        // Collect unique emails + phones from orders
        List<Object[]> orderContacts = orderRepo.findAllCustomerContacts();
        List<Map<String, Object>> orderEmails = new ArrayList<>();
        Set<String> seenEmails = subscribers.stream()
            .map(s -> (String) s.get("email"))
            .collect(Collectors.toSet());

        for (Object[] row : orderContacts) {
            String email = row[0] != null ? row[0].toString().toLowerCase() : null;
            String phone = row[1] != null ? row[1].toString() : null;
            String name  = row[2] != null ? row[2].toString() : null;

            if (email != null && !email.isBlank() && !seenEmails.contains(email)) {
                seenEmails.add(email);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("email", email);
                m.put("phone", phone);
                m.put("name", name);
                m.put("source", "ORDER");
                m.put("type", "CUSTOMER");
                orderEmails.add(m);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subscribers", subscribers);
        result.put("orderCustomers", orderEmails);
        result.put("totalEmails", seenEmails.size());

        return ResponseEntity.ok(result);
    }

    // ── Admin: unsubscribe ────────────────────────────────────────────────────

    @DeleteMapping("/admin/newsletter/subscribers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unsubscribe(@PathVariable Long id) {
        subscriberRepo.findById(id).ifPresent(s -> {
            s.setActive(false);
            subscriberRepo.save(s);
        });
        return ResponseEntity.ok(Map.of("message", "Unsubscribed"));
    }
}