package com.almang.inventory.store.dto.response;

import com.almang.inventory.store.domain.Store;

public record UpdateStoreResponse(
        Long storeId,
        String name,
        boolean isActivate
) {
    public static UpdateStoreResponse from(Store store) {
        return new UpdateStoreResponse(
                store.getId(),
                store.getName(),
                store.isActivate()
        );
    }
}
