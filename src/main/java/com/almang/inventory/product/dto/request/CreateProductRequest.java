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
        @Min(0) Integer costPrice,
        @Min(0) Integer retailPrice,
        @Min(0) Integer wholesalePrice,
        @NotBlank @PositiveOrZero BigDecimal reorderTriggerPoint
) {}
