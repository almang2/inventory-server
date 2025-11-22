package com.almang.inventory.order.dto.request;

import java.util.List;

public record CreateOrderRequest(
        Long vendorId,
        String orderMessage,
        Integer leadTime,
        List<CreateOrderItemRequest> orderItems
) {}
