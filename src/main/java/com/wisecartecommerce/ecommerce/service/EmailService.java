package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final OrderMapper orderMapper;  // Inject the mapper
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
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
            String subject = "Verify your email address";
            
            sendEmail(user.getEmail(), subject, content);
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
            String subject = "Password Reset Request";
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Password reset email sent to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user != null ? user.getEmail() : "unknown", e);
        }
    }
    
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        try {
            if (order == null) {
                log.error("Cannot send order confirmation email: order is null");
                return;
            }
            
            String recipientEmail = getRecipientEmail(order);
            if (recipientEmail == null) {
                log.error("Cannot send order confirmation email: no email found for order {}", order.getOrderNumber());
                return;
            }
            
            // Convert Order entity to OrderResponse DTO using the mapper
            OrderResponse orderResponse = orderMapper.toResponse(order);
            
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", orderResponse);  // Now using the DTO with productImage field
            context.setVariable("user", order.getUser());
            context.setVariable("orderUrl", frontendUrl + "/orders/" + order.getOrderNumber());
            
            String content = templateEngine.process("email/order-confirmation", context);
            String subject = "Order Confirmation - #" + order.getOrderNumber();
            
            sendEmail(recipientEmail, subject, content);
            log.info("Order confirmation email sent for order: {} to: {}", order.getOrderNumber(), recipientEmail);
            
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", 
                order != null ? order.getOrderNumber() : "unknown", e);
        }
    }
    
    @Async
    public void sendOrderStatusUpdateEmail(Order order) {
        try {
            if (order == null) {
                log.error("Cannot send order status update email: order is null");
                return;
            }
            
            String recipientEmail = getRecipientEmail(order);
            if (recipientEmail == null) {
                log.error("Cannot send order status update email: no email found for order {}", order.getOrderNumber());
                return;
            }
            
            // Convert Order entity to OrderResponse DTO using the mapper
            OrderResponse orderResponse = orderMapper.toResponse(order);
            
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", orderResponse);  // Now using the DTO
            context.setVariable("user", order.getUser());
            context.setVariable("orderUrl", frontendUrl + "/orders/" + order.getOrderNumber());
            
            String content = templateEngine.process("email/order-status-update", context);
            String subject = "Order Status Update - #" + order.getOrderNumber();
            
            sendEmail(recipientEmail, subject, content);
            log.info("Order status update email sent for order: {} to: {}", order.getOrderNumber(), recipientEmail);
            
        } catch (Exception e) {
            log.error("Failed to send order status update email for order: {}", 
                order != null ? order.getOrderNumber() : "unknown", e);
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
            String subject = "Welcome to Our E-Commerce Store!";
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Welcome email sent to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user != null ? user.getEmail() : "unknown", e);
        }
    }
    
    /**
     * Helper method to get the recipient email (either registered user or guest)
     */
    private String getRecipientEmail(Order order) {
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            return order.getUser().getEmail();
        } else if (order.getGuestEmail() != null) {
            return order.getGuestEmail();
        }
        return null;
    }
    
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
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            throw new MessagingException("Sender email address is not configured");
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        
        mailSender.send(message);
    }
}