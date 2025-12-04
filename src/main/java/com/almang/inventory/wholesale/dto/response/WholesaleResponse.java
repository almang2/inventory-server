package com.almang.inventory.wholesale.dto.response;

import com.almang.inventory.wholesale.domain.Wholesale;
import com.almang.inventory.wholesale.domain.WholesaleItem;
import com.almang.inventory.wholesale.domain.WholesaleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WholesaleResponse(
        Long wholesaleId,
        Long storeId,
        String orderReference,
        WholesaleStatus status,
        LocalDate releaseDate,
        boolean activated,
        List<WholesaleItemResponse> items,
        Integer totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WholesaleResponse from(Wholesale wholesale) {
        int totalAmount = wholesale.getItems().stream()
                .filter(item -> item.getAmount() != null)
                .mapToInt(WholesaleItem::getAmount)
                .sum();

        return new WholesaleResponse(
                wholesale.getId(),
                wholesale.getStore().getId(),
                wholesale.getOrderReference(),
                wholesale.getStatus(),
                wholesale.getReleaseDate(),
                wholesale.isActivated(),
                wholesale.getItems().stream()
                        .map(WholesaleItemResponse::from)
                        .toList(),
                totalAmount,
                wholesale.getCreatedAt(),
                wholesale.getUpdatedAt()
        );
    }
}

