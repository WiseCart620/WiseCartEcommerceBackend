package com.wisecartecommerce.ecommerce.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.mapper.OrderMapper;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final OrderMapper orderMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String resendFromEmail;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.from-email:sales@wisecart.ph}")
    private String brevoFromEmail;

    @Value("${app.contact.recipient-email}")
    private String contactRecipientEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.backend.url:https://backend.wisecart.ph}")
    private String backendUrl;



    @Async
    public void sendVerificationEmail(User user) {
        try {
            if (user == null || user.getEmail() == null) {
                log.error("Cannot send verification email: user or email is null");
                return;
            }
            Context context = new Context(Locale.getDefault());
            context.setVariable("user", user);
            context.setVariable("verificationUrl", frontendUrl + "/verify-email?token=" + user.getVerificationToken());
            String content = templateEngine.process("email/verification", context);
            sendEmail(user.getEmail(), "Verify your email address", content);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user != null ? user.getEmail() : "unknown", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            if (user == null || user.getEmail() == null) {
                log.error("Cannot send password reset email: user or email is null");
                return;
            }
            Context context = new Context(Locale.getDefault());
            context.setVariable("user", user);
            context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + resetToken);
            String content = templateEngine.process("email/password-reset", context);
            sendEmail(user.getEmail(), "Password Reset Request", content);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user != null ? user.getEmail() : "unknown", e);
        }
    }

    public void sendOrderConfirmationEmail(Order order) {
        if (order == null) {
            log.error("Cannot send order confirmation email: order is null");
            return;
        }
        String recipientEmail = getRecipientEmail(order);
        if (recipientEmail == null) {
            log.error("Cannot send order confirmation email: no email found for order {}", order.getOrderNumber());
            return;
        }
        OrderResponse orderResponse = orderMapper.toResponse(order);
        String orderNumber = order.getOrderNumber();
        String userFirstName = order.getUser() != null ? order.getUser().getFirstName() : null;

        dispatchOrderConfirmationEmail(orderResponse, recipientEmail, orderNumber, userFirstName);
    }

    @Async
    public void dispatchOrderConfirmationEmail(OrderResponse orderResponse, String recipientEmail,
            String orderNumber, String userFirstName) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", orderResponse);
            context.setVariable("userFirstName", userFirstName);
            context.setVariable("orderUrl", frontendUrl + "/orders/" + orderNumber);
            context.setVariable("imageBaseUrl", backendUrl);
            String content = templateEngine.process("email/order-confirmation", context);
            sendEmail(recipientEmail, "Order Confirmation - #" + orderNumber, content);
            log.info("Order confirmation email sent for order: {} to: {}", orderNumber, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", orderNumber, e);
        }
    }

    public void sendOrderStatusUpdateEmail(Order order) {
        if (order == null) {
            log.error("Cannot send order status update email: order is null");
            return;
        }
        String recipientEmail = getRecipientEmail(order);
        if (recipientEmail == null) {
            log.error("Cannot send order status update email: no email found for order {}", order.getOrderNumber());
            return;
        }
        // Fully materialize everything the template needs while the session
        // is still safely single-threaded.
        OrderResponse orderResponse = orderMapper.toResponse(order);
        String orderNumber = order.getOrderNumber();
        String userFirstName = order.getUser() != null ? order.getUser().getFirstName() : null;

        dispatchOrderStatusUpdateEmail(orderResponse, recipientEmail, orderNumber, userFirstName);
    }

    // Async dispatch — receives only plain data, nothing Hibernate-managed,
    // so it can safely run on a background thread without touching the session.
    @Async
    public void dispatchOrderStatusUpdateEmail(OrderResponse orderResponse, String recipientEmail,
            String orderNumber, String userFirstName) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", orderResponse);
            context.setVariable("userFirstName", userFirstName);
            context.setVariable("orderUrl", frontendUrl + "/orders/" + orderNumber);
            String content = templateEngine.process("email/order-status-update", context);
            sendEmail(recipientEmail, "Order Status Update - #" + orderNumber, content);
            log.info("Order status update email sent for order: {} to: {}", orderNumber, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send order status update email for order: {}", orderNumber, e);
        }
    }

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            if (user == null || user.getEmail() == null) {
                log.error("Cannot send welcome email: user or email is null");
                return;
            }
            Context context = new Context(Locale.getDefault());
            context.setVariable("user", user);
            context.setVariable("loginUrl", frontendUrl + "/login");
            String content = templateEngine.process("email/welcome", context);
            sendEmail(user.getEmail(), "Welcome to Our E-Commerce Store!", content);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user != null ? user.getEmail() : "unknown", e);
        }
    }

    @Async
    public void sendContactEmail(com.wisecartecommerce.ecommerce.Dto.Request.ContactRequest request) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("contact", request);
            String content = templateEngine.process("email/contact", context);
            sendEmail(contactRecipientEmail, "[WiseCart Contact] " + request.getSubject(), content);
            log.info("Contact email sent from: {} — Subject: {}", request.getEmail(), request.getSubject());
        } catch (Exception e) {
            log.error("Failed to send contact email from: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Async
    public void sendAdminReplyNotification(String customerEmail, String customerName,
            String originalSubject, String replyMessage) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("customerName", customerName);
            context.setVariable("originalSubject", originalSubject);
            context.setVariable("replyMessage", replyMessage);
            String content = templateEngine.process("email/contact-reply", context);
            sendEmail(customerEmail, "Re: [WiseCart] " + originalSubject, content);
            log.info("Admin reply notification sent to: {}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send admin reply notification to: {}", customerEmail, e);
            // Don't rethrow — reply was already saved to DB, email failure is non-fatal
        }
    }

    private String getRecipientEmail(Order order) {
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            return order.getUser().getEmail();
        } else if (order.getGuestEmail() != null) {
            return order.getGuestEmail();
        }
        return null;
    }

    // ── Core send logic: Resend first, Brevo as fallback ─────────────────
    private void sendEmail(String to, String subject, String content) throws MessagingException {
        if (to == null || to.trim().isEmpty()) {
            throw new MessagingException("Recipient email address is null or empty");
        }
        if (subject == null) {
            subject = "";
        }
        if (content == null) {
            content = "";
        }

        try {
            sendViaResend(to, subject, content);
            log.info("Email sent via Resend to: {}", to);
            return;
        } catch (Exception e) {
            log.warn("Resend failed for {}: {} — falling back to Brevo", to, e.getMessage());
        }

        try {
            sendViaBrevo(to, subject, content);
            log.info("Email sent via Brevo (fallback) to: {}", to);
            return;
        } catch (Exception e) {
            log.error("Brevo fallback also failed for {}: {}", to, e.getMessage(), e);
            throw new MessagingException("All email providers failed: " + e.getMessage());
        }
    }

    private void sendViaResend(String to, String subject, String content) throws Exception {
        Resend resend = new Resend(resendApiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(resendFromEmail)
                .to(to)
                .subject(subject)
                .html(content)
                .build();
        CreateEmailResponse response = resend.emails().send(params);
        if (response == null || response.getId() == null) {
            throw new RuntimeException("Resend returned no message id");
        }
    }

    private void sendViaBrevo(String to, String subject, String content) {
        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.set("accept", "application/json");

        Map<String, Object> sender = new HashMap<>();
        sender.put("email", brevoFromEmail);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", to);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", List.of(recipient));
        body.put("subject", subject);
        body.put("htmlContent", content);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, String.class); // throws on non-2xx
    }
}
