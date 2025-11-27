package com.almang.inventory.inventory.dto.request;

import com.almang.inventory.inventory.domain.InventoryMoveDirection;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InventoryMoveRequest(
        @NotNull BigDecimal quantity,
        @NotNull InventoryMoveDirection direction
) {}
