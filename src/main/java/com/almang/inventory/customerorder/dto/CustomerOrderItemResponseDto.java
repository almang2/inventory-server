package com.almang.inventory.customerorder.dto;

import com.almang.inventory.customerorder.domain.CustomerOrderItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerOrderItemResponseDto {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private String optionValue;
    private String variantCode;
    private String itemCode;

    public static CustomerOrderItemResponseDto from(CustomerOrderItem item) {
        return CustomerOrderItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productCode(item.getProductCode())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .optionValue(item.getOptionValue())
                .variantCode(item.getVariantCode())
                .itemCode(item.getItemCode())
                .build();
    }
}
