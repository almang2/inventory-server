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
                wholesaleItem.getInsufficientStock()
        );
    }
    
    public static WholesaleItemResponse from(WholesaleItem wholesaleItem, BigDecimal availableStock) {
        // 엔티티의 insufficientStock 플래그를 우선 사용, 없으면 availableStock으로 계산
        Boolean isStockInsufficient = wholesaleItem.getInsufficientStock();
        if (isStockInsufficient == null && availableStock != null) {
            isStockInsufficient = availableStock.compareTo(wholesaleItem.getQuantity()) < 0;
        }
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

