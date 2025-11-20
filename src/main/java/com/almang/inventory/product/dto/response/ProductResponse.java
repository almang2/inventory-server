package com.almang.inventory.product.dto.response;

import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.domain.ProductUnit;
import java.math.BigDecimal;

public record CreateProductResponse(
        String name,
        String code,
        ProductUnit unit,
        BigDecimal boxWeightG,
        boolean isActivate,
        int unitPerBox,
        BigDecimal unitWeightG,
        int costPrice,
        int retailPrice,
        int wholesalePrice,
        Long storeId,
        Long vendorId
) {
    public static CreateProductResponse from(Product product) {
        return new CreateProductResponse(
                product.getName(),
                product.getCode(),
                product.getUnit(),
                product.getBoxWeightG(),
                product.isActivate(),
                product.getUnitPerBox(),
                product.getUnitWeightG(),
                product.getCostPrice(),
                product.getRetailPrice(),
                product.getWholesalePrice(),
                product.getStore().getId(),
                product.getVendor().getId()
        );
    }
}
