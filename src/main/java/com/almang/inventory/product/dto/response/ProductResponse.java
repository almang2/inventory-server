package com.almang.inventory.product.dto.response;

import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.domain.ProductUnit;
import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String name,
        String cafe24Code,
        String posCode,
        ProductUnit unit,
        BigDecimal boxWeightG,
        boolean isActivated,
        int unitPerBox,
        BigDecimal unitWeightG,
        int costPrice,
        int retailPrice,
        int wholesalePrice,
        Long storeId,
        Long vendorId) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCafe24Code(),
                product.getPosCode(),
                product.getUnit(),
                product.getBoxWeightG(),
                product.isActivated(),
                product.getUnitPerBox(),
                product.getUnitWeightG(),
                product.getCostPrice(),
                product.getRetailPrice(),
                product.getWholesalePrice(),
                product.getStore().getId(),
                product.getVendor().getId());
    }
}
