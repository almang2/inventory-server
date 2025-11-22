package com.almang.inventory.order.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull Long productId,
        @NotNull int quantity,
        @NotNull int unitPrice,
        String note
) {}
