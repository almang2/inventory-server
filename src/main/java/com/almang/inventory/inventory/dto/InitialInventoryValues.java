package com.almang.inventory.inventory.dto;

import java.math.BigDecimal;

public record InitialInventoryValues(
        BigDecimal reorderTriggerPoint,
        BigDecimal displayStock,
        BigDecimal warehouseStock,
        BigDecimal outgoingReserved,
        BigDecimal incomingReserved
) {}
