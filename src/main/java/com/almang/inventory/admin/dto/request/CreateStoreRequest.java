package com.almang.inventory.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateStoreRequest(
        @NotBlank String name,
        @NotNull BigDecimal defaultCountCheckThreshold
) {}
