package com.almang.inventory.admin.dto.response;

import java.math.BigDecimal;

public record CreateStoreResponse(
        String name,
        boolean isActivate,
        BigDecimal defaultCountCheckThreshold
) {}
