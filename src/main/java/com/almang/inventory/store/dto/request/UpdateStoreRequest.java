package com.almang.inventory.store.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateStoreRequest(
        @Size(max = 20) String name,
        Boolean isActivate
) {}
