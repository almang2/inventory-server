package com.almang.inventory.admin.dto.response;

import com.almang.inventory.store.domain.Store;
import java.math.BigDecimal;

public record CreateStoreResponse(
        Long storeId,
        String name,
        boolean isActivate,
        BigDecimal defaultCountCheckThreshold
) {
    public static CreateStoreResponse from(Store store) {
        return new CreateStoreResponse(
                store.getId(),
                store.getName(),
                store.isActivate(),
                store.getDefaultCountCheckThreshold()
        );
    }
}
