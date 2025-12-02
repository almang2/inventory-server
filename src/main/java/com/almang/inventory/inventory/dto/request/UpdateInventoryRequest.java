package com.almang.inventory.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateInventoryRequest(
        @NotNull Long productId,
        @PositiveOrZero BigDecimal displayStock,
        @PositiveOrZero BigDecimal warehouseStock,
        @PositiveOrZero BigDecimal outgoingReserved,
        @PositiveOrZero BigDecimal incomingReserved,
        @PositiveOrZero BigDecimal reorderTriggerPoint
) {}
