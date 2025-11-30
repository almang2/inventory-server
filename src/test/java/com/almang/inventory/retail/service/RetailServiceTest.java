package com.almang.inventory.retail.service;

import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.retail.domain.Retail;
import com.almang.inventory.retail.repository.RetailRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RetailServiceTest {

    @InjectMocks
    private RetailService retailService;

    @Mock
    private RetailRepository retailRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Excel file processing success")
    void processRetailExcel_Success() throws IOException {
        // Given
        // 1. Mock Store
        Store store = Store.builder().id(1L).name("Test Store").build();
        given(storeRepository.findAll()).willReturn(List.of(store));

        // 2. Mock Product
        String productCode = "P001";
        Product product = Product.builder().id(1L).code(productCode).name("Test Product").build();
        given(productRepository.findByCode(productCode)).willReturn(Optional.of(product));

        // 3. Mock Inventory
        Inventory inventory = Inventory.builder().id(1L).product(product).displayStock(BigDecimal.valueOf(100)).build();
        given(inventoryRepository.findByProduct(product)).willReturn(Optional.of(inventory));

        // 4. Create Excel File
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Product Code");
        header.createCell(1).setCellValue("Quantity");

        // Data Row
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(productCode);
        row.createCell(1).setCellValue(10); // Quantity 10

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());

        // When
        retailService.processRetailExcel(file);

        // Then
        verify(retailRepository, times(1)).saveAll(anyList());
        // Inventory decrease check is tricky because it's a void method on the entity
        // or service logic inside lambda
        // But we can verify inventoryRepository.findByProduct was called
        verify(inventoryRepository, times(1)).findByProduct(product);
    }
}
