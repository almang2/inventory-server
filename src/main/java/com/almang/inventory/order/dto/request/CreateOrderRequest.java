package com.almang.inventory.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotNull Long vendorId,
        @NotBlank String orderMessage,
        Integer leadTime,
        @NotEmpty @Valid List<CreateOrderItemRequest> orderItems
) {}
