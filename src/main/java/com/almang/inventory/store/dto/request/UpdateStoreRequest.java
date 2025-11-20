package com.almang.inventory.store.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateStoreRequest(
        @Size(max = 20) String name,
        @DecimalMin("0.0") @DecimalMax("1.0")
        BigDecimal defaultCountCheckThreshold,
        Boolean isActivate
) {}
