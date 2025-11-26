package com.almang.inventory.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateInventoryRequest(
        @NotNull Long productId,
        BigDecimal displayStock,
        BigDecimal warehouseStock,
        BigDecimal outgoingReserved,
        BigDecimal incomingReserved,
        BigDecimal reorderTriggerPoint
) {}
