package com.wisecartecommerce.ecommerce.controller;

import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/webhook/flash")
@RequiredArgsConstructor
@Slf4j
public class FlashWebhookController {

    private final OrderRepository orderRepository;

    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> handleStatusWebhook(
            @RequestParam Map<String, String> params) {
        try {
            String pno        = params.get("data[pno]");
            String outTradeNo = params.get("data[outTradeNo]");
            String stateStr   = params.get("data[state]");
            String stateText  = params.get("data[stateText]");

            log.info("Flash status webhook: PNO={}, outTradeNo={}, state={} ({})",
                    pno, outTradeNo, stateStr, stateText);

            if (pno != null && stateStr != null) {
                updateOrderStatus(pno, outTradeNo, stateStr, stateText);
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
            String pno         = params.get("data[pno]");
            String outTradeNo  = params.get("data[outTradeNo]");
            String stateStr    = params.get("data[state]");
            String stateText   = params.get("data[stateText]");
            String routeAction = params.get("data[routedAction]");
            String message     = params.get("data[message]");

            log.info("Flash routes webhook: PNO={}, state={} ({}), action={}, msg={}",
                    pno, stateStr, stateText, routeAction, message);

            if (pno != null && stateStr != null) {
                updateOrderStatus(pno, outTradeNo, stateStr, stateText);
            }
        } catch (Exception e) {
            log.error("Flash routes webhook error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(successResponse());
    }

    private void updateOrderStatus(String pno, String outTradeNo,
                                   String stateStr, String stateText) {

        Optional<Order> orderOpt = orderRepository.findByTrackingNumber(pno);

        if (orderOpt.isEmpty() && outTradeNo != null) {
            try {
                Long orderId = Long.parseLong(outTradeNo);
                orderOpt = orderRepository.findById(orderId);
            } catch (NumberFormatException ignored) {}
        }

        if (orderOpt.isEmpty()) {
            log.warn("Flash webhook: order not found for PNO={} outTradeNo={}", pno, outTradeNo);
            return;
        }

        Order order = orderOpt.get();

        int flashState;
        try {
            flashState = Integer.parseInt(stateStr);
        } catch (NumberFormatException e) {
            log.warn("Flash webhook: invalid state value '{}'", stateStr);
            return;
        }


        OrderStatus newStatus = switch (flashState) {
            case 1   -> OrderStatus.PROCESSING;
            case 50  -> OrderStatus.SHIPPED;
            case 60  -> OrderStatus.SHIPPED;
            case 70  -> OrderStatus.OUT_FOR_DELIVERY;
            case 80  -> OrderStatus.DELIVERED;
            case 90  -> OrderStatus.SHIPPED;
            case 100 -> OrderStatus.RETURNED;
            default  -> null;
        };

        if (newStatus == null) {
            log.debug("Flash state {} has no mapping - skipping PNO={}", flashState, pno);
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

        log.info("Updating order {} status: {} -> {} (Flash state={} '{}')",
                order.getOrderNumber(), order.getStatus(), newStatus, flashState, stateText);

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED) {
            order.setCancelledAt(LocalDateTime.now());
        }

        orderRepository.save(order);
    }

    private boolean isDowngrade(OrderStatus current, OrderStatus next) {
        return statusRank(next) < statusRank(current);
    }

    private int statusRank(OrderStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case PENDING          -> 1;
            case PROCESSING       -> 2;
            case SHIPPED          -> 3;
            case OUT_FOR_DELIVERY -> 4;
            case DELIVERED        -> 5;
            case RETURNED         -> 5;
            case CANCELLED        -> 5;
            case REFUNDED         -> 5;
            case FAILED           -> 5;
        };
    }

    private Map<String, String> successResponse() {
        Map<String, String> resp = new HashMap<>();
        resp.put("errorCode", "1");
        resp.put("state", "success");
        return resp;
    }
}