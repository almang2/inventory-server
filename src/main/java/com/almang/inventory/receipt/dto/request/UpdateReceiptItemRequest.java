package com.almang.inventory.receipt.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateReceiptItemRequest(
        @NotNull Long receiptItemId,
        Long receiptId,
        Integer boxCount,
        BigDecimal measuredWeight,
        BigDecimal expectedQuantity,
        Integer actualQuantity,
        Integer unitPrice,
        String note
) {}
