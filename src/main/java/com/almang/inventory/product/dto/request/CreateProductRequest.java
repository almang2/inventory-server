package com.almang.inventory.product.dto.request;

import com.almang.inventory.product.domain.ProductUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotNull Long vendorId,
        @NotBlank String name,
        @NotBlank String code,
        @NotNull ProductUnit unit,
        BigDecimal boxWeightG,
        @Positive int unitPerBox,
        BigDecimal unitWeightG,
        @Min(0) int costPrice,
        @Min(0) int retailPrice,
        @Min(0) int wholesalePrice
) {}
