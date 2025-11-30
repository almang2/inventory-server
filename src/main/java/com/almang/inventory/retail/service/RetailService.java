package com.almang.inventory.retail.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.retail.domain.Retail;
import com.almang.inventory.retail.repository.RetailRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailService {

    private final RetailRepository retailRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public void processRetailExcel(MultipartFile file) {
        // 1. 유일한 상점 조회 (현재 상점은 하나뿐이라고 가정)
        Store store = storeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용
            List<Retail> retails = new ArrayList<>();

            // 헤더 행 스킵 (첫 번째 행이 헤더라고 가정)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // 엑셀 양식 가정:
                // Cell 0: 상품 코드 (Product Code)
                // Cell 1: 수량 (Quantity)
                // Cell 2: 적용 일시 (Applied At) - 선택 사항, 없으면 현재 시간

                String productCode = getCellValueAsString(row.getCell(0));
                if (productCode == null || productCode.isEmpty())
                    continue;

                BigDecimal quantity = getCellValueAsBigDecimal(row.getCell(1));
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                LocalDateTime appliedAt = LocalDateTime.now(); // 기본값
                // 날짜 파싱 로직이 필요하다면 추가 (Cell 2)

                // 2. 상품 조회
                Product product = productRepository.findByCode(productCode)
                        .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND,
                                "Product code not found: " + productCode));

                // 3. Retail 엔티티 생성
                Retail retail = Retail.builder()
                        .store(store)
                        .product(product)
                        .quantity(quantity)
                        .appliedAt(appliedAt)
                        .build();
                retails.add(retail);

                // 4. 재고 차감 (매대 재고 차감으로 가정)
                inventoryRepository.findByProduct(product).ifPresent(inventory -> {
                    inventory.decreaseDisplay(quantity);
                });
            }

            // 5. Retail 저장
            retailRepository.saveAll(retails);

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new RuntimeException("Failed to parse Excel file", e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else {
            return "";
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        } else {
            return BigDecimal.ZERO;
        }
    }
}
