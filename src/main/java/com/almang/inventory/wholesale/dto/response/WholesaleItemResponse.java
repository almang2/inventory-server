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
        String note,
        BigDecimal availableStock,
        Boolean isStockInsufficient
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
                wholesaleItem.getNote(),
                null,
                null
        );
    }
    
    public static WholesaleItemResponse from(WholesaleItem wholesaleItem, BigDecimal availableStock) {
        boolean isStockInsufficient = availableStock != null && availableStock.compareTo(wholesaleItem.getQuantity()) < 0;
        return new WholesaleItemResponse(
                wholesaleItem.getId(),
                wholesaleItem.getWholesale().getId(),
                wholesaleItem.getProduct().getId(),
                wholesaleItem.getProduct().getName(),
                wholesaleItem.getProduct().getCode(),
                wholesaleItem.getQuantity(),
                wholesaleItem.getUnitPrice(),
                wholesaleItem.getAmount(),
                wholesaleItem.getNote(),
                availableStock,
                isStockInsufficient
        );
    }
}

