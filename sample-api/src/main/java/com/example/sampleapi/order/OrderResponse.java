package com.example.sampleapi.order;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        OrderStatus status
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalPrice(),
                order.getStatus());
    }
}
