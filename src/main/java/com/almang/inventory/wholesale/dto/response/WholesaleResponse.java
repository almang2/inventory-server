package com.almang.inventory.wholesale.dto.response;

import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.wholesale.domain.Wholesale;
import com.almang.inventory.wholesale.domain.WholesaleItem;
import com.almang.inventory.wholesale.domain.WholesaleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    public static WholesaleResponse fromWithStockInfo(Wholesale wholesale, InventoryRepository inventoryRepository) {
        int totalAmount = wholesale.getItems().stream()
                .filter(item -> item.getAmount() != null)
                .mapToInt(WholesaleItem::getAmount)
                .sum();

        // 모든 productId 수집
        List<Long> productIds = wholesale.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList();

        // 배치로 재고 정보 조회
        Map<Long, Inventory> inventoryMap = inventoryRepository.findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

        // 재고 정보를 포함한 응답 생성
        List<WholesaleItemResponse> itemsWithStock = wholesale.getItems().stream()
                .map(item -> {
                    Inventory inventory = inventoryMap.get(item.getProduct().getId());
                    BigDecimal availableStock = inventory != null ? inventory.getAvailableStock() : null;
                    return WholesaleItemResponse.from(item, availableStock);
                })
                .toList();

        return new WholesaleResponse(
                wholesale.getId(),
                wholesale.getStore().getId(),
                wholesale.getOrderReference(),
                wholesale.getStatus(),
                wholesale.getReleaseDate(),
                wholesale.isActivated(),
                itemsWithStock,
                totalAmount,
                wholesale.getCreatedAt(),
                wholesale.getUpdatedAt()
        );
    }
}

