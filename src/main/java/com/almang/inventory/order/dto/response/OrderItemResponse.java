package com.almang.inventory.order.dto.response;

import com.almang.inventory.order.domain.OrderItem;

public record OrderItemResponse(
        Long orderItemId,
        Long orderId,
        Long productId,
        Integer quantity,
        Integer unitPrice,
        Integer amount
) {
    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getAmount()
        );
    }
}
