package com.almang.inventory.order.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderItemRequest(
        @NotNull Long orderItemId,
        Long productId,
        Integer quantity,
        Integer unitPrice,
        String note
) {}
