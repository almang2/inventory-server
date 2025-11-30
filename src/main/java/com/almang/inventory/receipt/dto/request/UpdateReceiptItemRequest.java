package com.almang.inventory.receipt.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateReceiptItemRequest(
        @NotNull Long receiptItemId,
        Long receiptId,
        Integer actualQuantity,
        Integer unitPrice,
        String note
) {}
