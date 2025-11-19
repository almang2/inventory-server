package com.almang.inventory.user.dto.response;

import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;

public record UserProfileResponse(
        String username,
        String name,
        UserRole role,
        String storeName
) {
    public static UserProfileResponse from(User user, Store store) {
        return new UserProfileResponse(
                user.getUsername(),
                user.getName(),
                user.getRole(),
                store.getName()
        );
    }
}
