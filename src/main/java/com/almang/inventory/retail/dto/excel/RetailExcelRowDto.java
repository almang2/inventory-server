package com.almang.inventory.retail.dto.excel;

import java.math.BigDecimal;

public record RetailExcelRowDto(
        int rowIndex,
        String code,
        String productName,
        BigDecimal quantity,
        Integer actualSales
) {}
