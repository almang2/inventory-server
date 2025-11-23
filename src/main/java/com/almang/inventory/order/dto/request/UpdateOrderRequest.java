package com.almang.inventory.order.dto.request;

import com.almang.inventory.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record UpdateOrderRequest(
        @NotNull Long vendorId,
        OrderStatus orderStatus,
        String orderMessage,
        Integer leadTime,
        LocalDate quoteReceivedAt,
        LocalDate depositConfirmedAt,
        Boolean activated,
        @Valid List<UpdateOrderItemRequest> orderItems
) {}
