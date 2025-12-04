package com.almang.inventory.wholesale.dto.response;

import com.almang.inventory.wholesale.domain.WholesaleItem;
import java.math.BigDecimal;

public record WholesaleItemResponse(
        Long wholesaleItemId,
        Long wholesaleId,
        Long productId,
        String productName,
        String productCode,
        BigDecimal quantity,
        Integer unitPrice,
        Integer amount,
        String note
) {
    public static WholesaleItemResponse from(WholesaleItem wholesaleItem) {
        return new WholesaleItemResponse(
                wholesaleItem.getId(),
                wholesaleItem.getWholesale().getId(),
                wholesaleItem.getProduct().getId(),
                wholesaleItem.getProduct().getName(),
                wholesaleItem.getProduct().getCode(),
                wholesaleItem.getQuantity(),
                wholesaleItem.getUnitPrice(),
                wholesaleItem.getAmount(),
                wholesaleItem.getNote()
        );
    }
}

