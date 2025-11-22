package com.almang.inventory.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderTemplateRequest(
        @NotBlank String title,
        @NotBlank String body
) {}
