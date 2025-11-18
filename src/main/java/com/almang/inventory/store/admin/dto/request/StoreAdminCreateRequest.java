package com.almang.inventory.store.admin.dto.request;

public record StoreAdminCreateRequest(
        String username,
        String name,
        Long storeId
) {}
