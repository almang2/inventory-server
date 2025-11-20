package com.almang.inventory.product.dto.request;

import com.almang.inventory.product.domain.ProductUnit;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotNull Long vendorId,
        String name,
        String code,
        ProductUnit unit,
        BigDecimal boxWeightG,
        Integer unitPerBox,
        BigDecimal unitWeightG,
        Boolean isActivate,
        Integer costPrice,
        Integer retailPrice,
        Integer wholesalePrice
) {}
