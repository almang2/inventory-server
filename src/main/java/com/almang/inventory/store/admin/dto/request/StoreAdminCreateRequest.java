package com.almang.inventory.store.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreAdminCreateRequest(
        @NotBlank @Size(max = 20) String username,
        @NotBlank @Size(min = 2, max = 20) String name,
        @NotNull Long storeId
) {}
