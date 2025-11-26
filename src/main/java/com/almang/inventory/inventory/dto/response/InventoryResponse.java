package com.almang.inventory.inventory.dto.response;

import com.almang.inventory.inventory.domain.Inventory;
import java.math.BigDecimal;

public record InventoryResponse(
        Long inventoryId,
        Long productId,
        BigDecimal displayStock,
        BigDecimal warehouseStock,
        BigDecimal outgoingReserved,
        BigDecimal incomingReserved,
        BigDecimal reorderTriggerPoint
) {
    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getDisplayStock(),
                inventory.getWarehouseStock(),
                inventory.getOutgoingReserved(),
                inventory.getIncomingReserved(),
                inventory.getReorderTriggerPoint()
        );
    }
}
