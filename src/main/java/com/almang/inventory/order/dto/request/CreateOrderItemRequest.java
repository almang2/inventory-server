package com.almang.inventory.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull Long productId,
        @Positive int quantity,
        String note
) {}
