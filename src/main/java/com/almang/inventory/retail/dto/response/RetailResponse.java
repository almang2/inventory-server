package com.almang.inventory.retail.dto.response;

import com.almang.inventory.retail.domain.Retail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RetailResponse(
        Long retailId,
        Long storeId,
        Long productId,
        String productCode,
        String productName,
        LocalDate soldDate,
        BigDecimal quantity,
        Integer actualSales,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RetailResponse from(Retail retail) {
        return new RetailResponse(
                retail.getId(),
                retail.getStore() != null ? retail.getStore().getId() : null,
                retail.getProduct().getId(),
                retail.getProductCode(),
                retail.getProductName(),
                retail.getSoldDate(),
                retail.getQuantity(),
                retail.getActualSales(),
                retail.getCreatedAt(),
                retail.getUpdatedAt()
        );
    }
}

