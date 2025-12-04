package com.almang.inventory.order.dto.response;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.domain.OrderItem;

public record OrderItemResponse(
        Long orderItemId,
        Long orderId,
        Long productId,
        Integer quantity,
        Integer unitPrice,
        Integer amount,
        String note,
        String productName,
        String productCode
) {
    public static OrderItemResponse from(OrderItem orderItem) {
        if (orderItem.getOrder() == null || orderItem.getProduct() == null) {
            throw new BaseException(ErrorCode.ORDER_ITEM_MUST_HAVE_ORDER_AND_PRODUCT);
        }
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getAmount(),
                orderItem.getNote(),
                orderItem.getProduct().getName(),
                orderItem.getProduct().getCode()
        );
    }
}
