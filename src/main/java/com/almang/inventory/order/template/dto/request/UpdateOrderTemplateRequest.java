package com.almang.inventory.order.template.dto.request;

public record UpdateOrderTemplateRequest(
        String title,
        String body,
        Boolean activated
) {}
