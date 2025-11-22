package com.almang.inventory.order.dto.request;

public record CreateOrderItemRequest(
        Long productId,
        int quantity,
        int unitPrice,
        String note
) {}
