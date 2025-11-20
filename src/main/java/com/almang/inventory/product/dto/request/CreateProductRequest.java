package com.almang.inventory.product.dto.request;

import com.almang.inventory.product.domain.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotNull ProductUnit unit,
        BigDecimal boxWeightG,
        int unitPerBox,
        BigDecimal unitWeightG,
        int costPrice,
        int retailPrice,
        int wholesalePrice
) {}
