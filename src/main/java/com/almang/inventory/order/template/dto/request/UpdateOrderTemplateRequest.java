package com.almang.inventory.order.template.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderTemplateRequest(
        @NotNull Long vendorId,
        String title,
        String body,
        Boolean activated
) {}
