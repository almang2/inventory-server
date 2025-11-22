package com.almang.inventory.order.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotNull Long vendorId,
        @NotNull String orderMessage,
        Integer leadTime,
        @NotNull List<CreateOrderItemRequest> orderItems
) {}
