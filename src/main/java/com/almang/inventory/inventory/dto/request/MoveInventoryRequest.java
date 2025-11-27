package com.almang.inventory.inventory.dto.request;

import com.almang.inventory.inventory.domain.InventoryMoveDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record MoveInventoryRequest(
        @NotNull @Positive BigDecimal quantity,
        @NotNull InventoryMoveDirection direction
) {}
