package com.almang.inventory.retail.dto.upload;

import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.retail.dto.excel.RetailExcelRowDto;
import java.util.List;
import java.util.Map;

public record UploadPreparationResult(
        List<RetailExcelRowDto> rows,
        List<SkippedRow> skippedRows,
        Map<String, Product> productByCode,
        Map<Long, Inventory> inventoryByProductId
) {}
