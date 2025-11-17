package com.almang.inventory.user.dto.request;

import com.almang.inventory.user.domain.UserRole;

public record SignUpRequest(
        String username,
        String password,
        String name,
        UserRole role,
        Long storeId
) {}
