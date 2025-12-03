package com.almang.inventory.inventory.domain;

import java.math.BigDecimal;

public enum InventoryStatus {
    NORMAL,
    LOW,
    OUT_OF_STOCK;

    public static InventoryStatus from(BigDecimal warehouseStock, BigDecimal reorderTriggerPoint) {
        if (warehouseStock.compareTo(BigDecimal.ZERO) == 0) {
            return OUT_OF_STOCK;
        }
        if (warehouseStock.compareTo(reorderTriggerPoint) <= 0) {
            return LOW;
        }
        return NORMAL;
    }
}
