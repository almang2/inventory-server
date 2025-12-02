package com.almang.inventory.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateStoreRequest(
        @NotBlank String name
) {}
