package com.almang.inventory.inventory.dto.response;

import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.domain.InventoryStatus;
import com.almang.inventory.product.domain.Product;
import java.math.BigDecimal;

public record InventoryResponse(
        Long inventoryId,
        Long productId,
        String productName,
        String productCode,
        BigDecimal displayStock,
        BigDecimal warehouseStock,
        BigDecimal outgoingReserved,
        BigDecimal incomingReserved,
        BigDecimal reorderTriggerPoint,
        InventoryStatus inventoryStatus
) {
    public static InventoryResponse from(Inventory inventory) {
        Product product = inventory.getProduct();

        return new InventoryResponse(
                inventory.getId(),
                product.getId(),
                product.getName(),
                product.getCode(),
                inventory.getDisplayStock(),
                inventory.getWarehouseStock(),
                inventory.getOutgoingReserved(),
                inventory.getIncomingReserved(),
                inventory.getReorderTriggerPoint(),
                InventoryStatus.from(
                        inventory.getDisplayStock(),
                        inventory.getWarehouseStock(),
                        inventory.getReorderTriggerPoint()
                )
        );
    }
}
