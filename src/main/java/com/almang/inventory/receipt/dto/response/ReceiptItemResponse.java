package com.almang.inventory.receipt.dto.response;

import com.almang.inventory.receipt.domain.ReceiptItem;
import java.math.BigDecimal;

public record ReceiptItemResponse(
        Long receiptItemId,
        Long receiptId,
        Long productId,
        Integer expectedQuantity,
        Integer actualQuantity,
        Integer unitPrice,
        Integer amount,
        String note,
        String productName,
        String productCode
) {
    public static ReceiptItemResponse from(ReceiptItem receiptItem) {
        return new ReceiptItemResponse(
                receiptItem.getId(),
                receiptItem.getReceipt().getId(),
                receiptItem.getProduct().getId(),
                receiptItem.getExpectedQuantity(),
                receiptItem.getActualQuantity(),
                receiptItem.getUnitPrice(),
                receiptItem.getAmount(),
                receiptItem.getNote(),
                receiptItem.getProduct().getName(),
                receiptItem.getProduct().getCode()
        );
    }
}
