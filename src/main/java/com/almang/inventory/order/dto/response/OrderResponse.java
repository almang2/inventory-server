package com.almang.inventory.order.dto.response;

import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long storeId,
        Long vendorId,
        String orderMessage,
        OrderStatus orderStatus,
        Integer leadTime,
        LocalDate expectedArrival,
        LocalDateTime quoteReceivedAt,
        LocalDateTime depositConfirmedAt,
        boolean activated,
        Integer totalPrice,
        List<OrderItemResponse> orderItems
) {
    public static OrderResponse of(Order order, List<OrderItem> orderItems) {
        return new OrderResponse(
                order.getId(),
                order.getStore().getId(),
                order.getVendor().getId(),
                order.getOrderMessage(),
                order.getStatus(),
                order.getLeadTime(),
                order.getExpectedArrival(),
                order.getQuoteReceivedAt(),
                order.getDepositConfirmedAt(),
                order.isActivated(),
                order.getTotalPrice(),
                orderItems.stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}
