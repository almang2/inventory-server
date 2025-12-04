package com.almang.inventory.product.dto.request;

import com.almang.inventory.product.domain.ProductUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotNull Long vendorId,
        @NotBlank String name,
        @NotBlank String code,
        @NotNull ProductUnit unit,
        @NotNull @PositiveOrZero Integer costPrice,
        @NotNull @PositiveOrZero Integer retailPrice,
        @NotNull @PositiveOrZero Integer wholesalePrice,
        @NotNull @PositiveOrZero BigDecimal reorderTriggerPoint,
        @NotNull @PositiveOrZero BigDecimal displayStock,
        @NotNull @PositiveOrZero BigDecimal warehouseStock,
        @NotNull @PositiveOrZero BigDecimal outgoingReserved,
        @NotNull @PositiveOrZero BigDecimal incomingReserved
) {}
