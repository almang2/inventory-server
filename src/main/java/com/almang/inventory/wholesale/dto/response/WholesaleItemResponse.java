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
        // 재고 부족 여부 결정:
        // 1. availableStock이 null이면 isStockInsufficient도 null (재고 정보 없음)
        // 2. availableStock이 있으면 계산 (엔티티 플래그보다 최신 정보 우선)
        Boolean isStockInsufficient = availableStock != null
                ? availableStock.compareTo(wholesaleItem.getQuantity()) < 0
                : null;
        
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

