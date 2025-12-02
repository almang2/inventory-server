package com.almang.inventory.inventory.domain;

import java.math.BigDecimal;

public enum InventoryStatus {
    NORMAL,
    LOW,
    OUT_OF_STOCK;

    public static InventoryStatus from(BigDecimal displayStock, BigDecimal warehouseStock, BigDecimal reorderTriggerPoint) {
        BigDecimal total = displayStock.add(warehouseStock);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return OUT_OF_STOCK;
        }
        if (total.compareTo(reorderTriggerPoint) <= 0) {
            return LOW;
        }
        return NORMAL;
    }
}
