package com.almang.inventory.store.dto.response;

import com.almang.inventory.store.domain.Store;
import java.math.BigDecimal;

public record UpdateStoreResponse(
        Long storeId,
        String name,
        boolean isActivate,
        BigDecimal defaultCountCheckThreshold
) {
    public static UpdateStoreResponse from(Store store) {
        return new UpdateStoreResponse(
                store.getId(),
                store.getName(),
                store.isActivate(),
                store.getDefaultCountCheckThreshold()
        );
    }
}
