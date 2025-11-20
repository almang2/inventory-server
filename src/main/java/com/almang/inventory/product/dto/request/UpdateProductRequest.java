package com.almang.inventory.product.dto.request;

import com.almang.inventory.product.domain.ProductUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotNull Long vendorId,
        String name,
        String code,
        ProductUnit unit,
        BigDecimal boxWeightG,
        @Positive Integer unitPerBox,
        BigDecimal unitWeightG,
        Boolean isActivated,
        @Min(0) Integer costPrice,
        @Min(0) Integer retailPrice,
        @Min(0) Integer wholesalePrice
) {}
