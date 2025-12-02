package com.almang.inventory.product.dto.request;

import com.almang.inventory.product.domain.ProductUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProductRequest(
        @NotNull Long vendorId,
        String name,
        String code,
        ProductUnit unit,
        Boolean isActivated,
        @Min(0) Integer costPrice,
        @Min(0) Integer retailPrice,
        @Min(0) Integer wholesalePrice
) {}
