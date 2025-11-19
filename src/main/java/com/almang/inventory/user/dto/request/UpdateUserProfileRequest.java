package com.almang.inventory.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank String name
) {}
