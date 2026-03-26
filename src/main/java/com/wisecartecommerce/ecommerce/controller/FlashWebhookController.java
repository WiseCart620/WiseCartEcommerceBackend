package com.wisecartecommerce.ecommerce.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.service.impl.PaymentServiceImpl;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/webhook/flash")
@RequiredArgsConstructor
@Slf4j
public class FlashWebhookController {

    private final OrderRepository orderRepository;
    private final PaymentServiceImpl paymentService;

    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> handleStatusWebhook(
            @RequestParam Map<String, String> params) {
        try {
            String pno = params.get("data[pno]");
            String outTradeNo = params.get("data[outTradeNo]");
            String stateStr = params.get("data[state]");
            String stateText = params.get("data[stateText]");

            log.info("Flash status webhook: PNO={}, outTradeNo={}, state={} ({})",
                    pno, outTradeNo, stateStr, stateText);

            if (pno != null && stateStr != null) {
                updateOrderFromWebhook(pno, outTradeNo, stateStr, stateText);
            }
        } catch (Exception e) {
            log.error("Flash webhook processing error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(successResponse());
    }

    @PostMapping("/routes")
    public ResponseEntity<Map<String, String>> handleRoutesWebhook(
            @RequestParam Map<String, String> params) {
        try {
            String pno = params.get("data[pno]");
            String outTradeNo = params.get("data[outTradeNo]");
            String stateStr = params.get("data[state]");
            String stateText = params.get("data[stateText]");
            String routeAction = params.get("data[routedAction]");
            String message = params.get("data[message]");

            log.info("Flash routes webhook: PNO={}, state={} ({}), action={}, msg={}",
                    pno, stateStr, stateText, routeAction, message);

            if (pno != null && stateStr != null) {
                updateOrderFromWebhook(pno, outTradeNo, stateStr, stateText);
            }
        } catch (Exception e) {
            log.error("Flash routes webhook error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(successResponse());
    }


    private void updateOrderFromWebhook(String pno, String outTradeNo,
            String stateStr, String stateText) {
        Order order = resolveOrder(pno, outTradeNo);
        if (order == null)
            return;

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Flash webhook: skipping update for locally-cancelled order PNO={}", pno);
            return;
        }

        int flashState;
        try {
            flashState = Integer.parseInt(stateStr.trim());
        } catch (NumberFormatException e) {
            log.warn("Flash webhook: invalid state value '{}'", stateStr);
            return;
        }

        OrderStatus newStatus = switch (flashState) {
            case 1 -> OrderStatus.PROCESSING;
            case 2 -> OrderStatus.SHIPPED;
            case 3 -> OrderStatus.OUT_FOR_DELIVERY;
            case 4 -> OrderStatus.SHIPPED;
            case 5 -> OrderStatus.DELIVERED;
            case 6 -> OrderStatus.SHIPPED;
            case 7 -> OrderStatus.RETURNED;
            case 8 -> OrderStatus.CANCELLED;
            case 9 -> OrderStatus.CANCELLED;
            case 98 -> OrderStatus.CANCELLED;
            case 99 -> OrderStatus.PROCESSING;
            default -> null;
        };

        if (newStatus == null) {
            log.debug("Flash webhook state {} has no mapping - skipping PNO={}", flashState, pno);
            return;
        }

        if (isDowngrade(order.getStatus(), newStatus)) {
            log.info("Flash webhook: skipping downgrade {} -> {} for PNO={}",
                    order.getStatus(), newStatus, pno);
            return;
        }

        if (newStatus.equals(order.getStatus())) {
            log.debug("Flash webhook: status unchanged ({}) for PNO={}", newStatus, pno);
            return;
        }

        log.info("Updating order {} status: {} -> {} (Flash webhook state={} '{}')",
                order.getOrderNumber(), order.getStatus(), newStatus, flashState, stateText);

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());

            if (isCod(order.getPaymentMethod())) {
                try {
                    paymentService.completeCodPaymentOnDelivery(order);
                } catch (Exception e) {
                    log.error("Failed to complete COD payment for order {}: {}",
                            order.getOrderNumber(), e.getMessage());
                }
            }
        }

        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED) {
            order.setCancelledAt(LocalDateTime.now());
        }

        orderRepository.save(order);
    }

    private boolean isCod(String paymentMethod) {
        return "COD".equalsIgnoreCase(paymentMethod) ||
                "CASH_ON_DELIVERY".equalsIgnoreCase(paymentMethod);
    }

    private Order resolveOrder(String pno, String outTradeNo) {
        Optional<Order> orderOpt = orderRepository.findByTrackingNumber(pno);

        if (orderOpt.isEmpty() && outTradeNo != null) {
            try {
                Long orderId = Long.parseLong(outTradeNo);
                orderOpt = orderRepository.findById(orderId);
            } catch (NumberFormatException ignored) {
            }
        }

        if (orderOpt.isEmpty()) {
            log.warn("Flash webhook: order not found for PNO={} outTradeNo={}", pno, outTradeNo);
            return null;
        }

        return orderOpt.get();
    }

    private boolean isDowngrade(OrderStatus current, OrderStatus next) {
        return statusRank(next) < statusRank(current);
    }

    private int statusRank(OrderStatus status) {
        if (status == null)
            return 0;
        return switch (status) {
            case PENDING -> 1;
            case PROCESSING -> 2;
            case SHIPPED -> 3;
            case OUT_FOR_DELIVERY -> 4;
            case DELIVERED -> 5;
            case RETURNED -> 5;
            case CANCELLED -> 5;
            case REFUNDED -> 5;
            case FAILED -> 5;
        };
    }

    private Map<String, String> successResponse() {
        Map<String, String> resp = new HashMap<>();
        resp.put("errorCode", "1");
        resp.put("state", "success");
        return resp;
    }
}