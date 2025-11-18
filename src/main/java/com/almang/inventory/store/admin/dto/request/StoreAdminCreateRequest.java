package com.almang.inventory.store.admin.dto.request;

import com.almang.inventory.user.domain.UserRole;

public record StoreAdminCreateRequest(
        String username,
        String name,
        Long storeId
) {}
