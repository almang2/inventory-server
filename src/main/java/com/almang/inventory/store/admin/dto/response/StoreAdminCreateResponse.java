package com.almang.inventory.store.admin.dto.response;

public record StoreAdminCreateResponse(
        Long userId,
        String username,
        String password,
        String name,
        Long storeId
) {}
