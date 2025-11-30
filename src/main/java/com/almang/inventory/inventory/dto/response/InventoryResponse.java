package com.almang.inventory.inventory.dto.response;

import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.product.domain.Product;
import java.math.BigDecimal;

public record InventoryResponse(
        Long inventoryId,
        Long productId,
        String productName,
        String cafe24Code,
        String posCode,
        BigDecimal displayStock,
        BigDecimal warehouseStock,
        BigDecimal outgoingReserved,
        BigDecimal incomingReserved,
        BigDecimal reorderTriggerPoint) {
    public static InventoryResponse from(Inventory inventory) {
        Product product = inventory.getProduct();

        return new InventoryResponse(
                inventory.getId(),
                product.getId(),
                product.getName(),
                product.getCafe24Code(),
                product.getPosCode(),
                inventory.getDisplayStock(),
                inventory.getWarehouseStock(),
                inventory.getOutgoingReserved(),
                inventory.getIncomingReserved(),
                inventory.getReorderTriggerPoint());
    }
}
