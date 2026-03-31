package com.almang.inventory.receipt.dto.response;

import com.almang.inventory.receipt.domain.Receipt;
import com.almang.inventory.receipt.domain.ReceiptItem;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import java.time.LocalDate;
import java.util.List;

public record ReceiptResponse(
        Long receiptId,
        Long storeId,
        Long orderId,
        LocalDate receiptDate,
        ReceiptStatus status,
        boolean activated,
        List<ReceiptItemResponse> receiptItems
) {
    public static ReceiptResponse of(Receipt receipt, List<ReceiptItem> receiptItems) {
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getStore().getId(),
                receipt.getOrder().getId(),
                receipt.getReceiptDate(),
                receipt.getStatus(),
                receipt.isActivated(),
                receiptItems.stream()
                        .map(ReceiptItemResponse::from)
                        .toList()
        );
    }
}
