package com.almang.inventory.order.template.dto.response;

import com.almang.inventory.order.template.domain.OrderTemplate;

public record OrderTemplateResponse(
        Long vendorId,
        String title,
        String body,
        boolean activated
) {
    public static OrderTemplateResponse from(OrderTemplate orderTemplate) {
        return new OrderTemplateResponse(
                orderTemplate.getVendor().getId(),
                orderTemplate.getTitle(),
                orderTemplate.getBody(),
                orderTemplate.isActivated()
        );
    }
}
