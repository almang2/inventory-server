package com.almang.inventory.product.dto.response;

import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.domain.ProductUnit;

public record ProductResponse(
        Long productId,
        String name,
        String code,
        ProductUnit unit,
        boolean isActivated,
        Integer costPrice,
        Integer retailPrice,
        Integer wholesalePrice,
        Long storeId,
        Long vendorId
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCode(),
                product.getUnit(),
                product.isActivated(),
                product.getCostPrice(),
                product.getRetailPrice(),
                product.getWholesalePrice(),
                product.getStore().getId(),
                product.getVendor().getId()
        );
    }
}
