package com.wisecartecommerce.ecommerce.mapper;

import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.OrderItem;
import com.wisecartecommerce.ecommerce.entity.Address;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    
    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
            .map(this::toOrderItemResponse)
            .collect(Collectors.toList());
        
        return OrderResponse.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .userEmail(order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail())
            .userName(getUserName(order))
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .discountAmount(order.getDiscountAmount())
            .shippingAmount(order.getShippingAmount())
            .taxAmount(order.getTaxAmount())
            .finalAmount(order.getFinalAmount())
            .shippingAddress(toAddressResponse(order.getShippingAddress()))
            .billingAddress(toAddressResponse(order.getBillingAddress()))
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .couponCode(order.getCouponCode())
            .trackingNumber(order.getTrackingNumber())
            .shippingCarrier(order.getShippingCarrier())
            .estimatedDelivery(order.getEstimatedDelivery())
            .deliveredAt(order.getDeliveredAt())
            .items(itemResponses)
            .notes(order.getNotes())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
    
    private OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderResponse.OrderItemResponse.builder()
            .id(item.getId())
            .productId(item.getProduct().getId())
            .productName(item.getProduct().getName())
            .productImage(item.getProduct().getImageUrl())
            .price(item.getPrice())
            .quantity(item.getQuantity())
            .subtotal(item.getSubtotal())
            .variationName(item.getVariation() != null ? item.getVariation().getName() : null)
            .build();
    }
    
    private OrderResponse.AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        
        return OrderResponse.AddressResponse.builder()
            .id(address.getId())
            .firstName(address.getFirstName())
            .lastName(address.getLastName())
            .phone(address.getPhone())
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .city(address.getCity())
            .state(address.getState())
            .postalCode(address.getPostalCode())
            .country(address.getCountry())
            .companyName(address.getCompanyName())
            .build();
    }
    
    private String getUserName(Order order) {
        if (order.getUser() != null) {
            return order.getUser().getFirstName() + " " + order.getUser().getLastName();
        } else if (order.getGuestFirstName() != null) {
            return order.getGuestFirstName() + " " + order.getGuestLastName();
        }
        return "Customer";
    }
}   