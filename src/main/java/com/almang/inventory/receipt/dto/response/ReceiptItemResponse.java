package com.almang.inventory.receipt.dto.response;

import com.almang.inventory.receipt.domain.ReceiptItem;
import java.math.BigDecimal;

public record ReceiptItemResponse(
        Long receiptItemId,
        Long receiptId,
        Long productId,
        Integer boxCount,
        BigDecimal measuredWeight,
        BigDecimal expectedQuantity,
        Integer actualQuantity,
        Integer unitPrice,
        Integer amount,
        BigDecimal errorRate,
        String note
) {
    public static ReceiptItemResponse from(ReceiptItem receiptItem) {
        return new ReceiptItemResponse(
                receiptItem.getId(),
                receiptItem.getReceipt().getId(),
                receiptItem.getProduct().getId(),
                receiptItem.getBoxCount(),
                receiptItem.getMeasuredWeight(),
                receiptItem.getExpectedQuantity(),
                receiptItem.getActualQuantity(),
                receiptItem.getAmount(),
                receiptItem.getUnitPrice(),
                receiptItem.getErrorRate(),
                receiptItem.getNote()
        );
    }
}
