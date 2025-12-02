package com.almang.inventory.retail.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.retail.domain.Retail;
import com.almang.inventory.retail.dto.response.RetailResponse;
import com.almang.inventory.retail.repository.RetailRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final UserContextProvider userContextProvider;

    @Transactional
    public void processRetailExcel(MultipartFile file) {
        // 1. 상점 조회 (없으면 null)
        Store store = storeRepository.findAll().stream().findFirst().orElse(null);

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용
            List<Retail> retails = new ArrayList<>();

            // 판매일자: 업로드 시점의 날짜 사용 (당일매출종합현황이므로 오늘 날짜)
            LocalDate soldDate = LocalDate.now();

            // 중복 체크: 같은 날짜에 이미 데이터가 있는지 확인 (스토어가 있을 때만)
            if (store != null) {
                List<Retail> existingRetails = retailRepository.findAllByStoreIdAndSoldDate(store.getId(), soldDate);
                if (!existingRetails.isEmpty()) {
                    log.warn("[RetailService] 해당 날짜({})에 이미 소매 데이터가 존재합니다. 기존 데이터를 삭제하고 새로 저장합니다. - count: {}",
                            soldDate, existingRetails.size());
                    retailRepository.deleteAll(existingRetails);
                }
            } else {
                // 스토어가 없을 때는 날짜만으로 중복 체크
                List<Retail> existingRetails = retailRepository.findAllBySoldDate(soldDate);
                if (!existingRetails.isEmpty()) {
                    log.warn("[RetailService] 해당 날짜({})에 이미 소매 데이터가 존재합니다. 기존 데이터를 삭제하고 새로 저장합니다. - count: {}",
                            soldDate, existingRetails.size());
                    retailRepository.deleteAll(existingRetails);
                }
            }

            // 헤더 행 스킵 (첫 번째 행이 헤더라고 가정)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // 엑셀 양식:
                // Cell 0: No. (순번)
                // Cell 1: 상품 코드 (Product Code)
                // Cell 2: 상품명 (Product Name)
                // Cell 3: 수량 (Quantity)
                // Cell 4: 실매출 (Actual Sales)

                String code = getCellValueAsString(row.getCell(1));
                if (code == null || code.isEmpty())
                    continue;

                String productName = getCellValueAsString(row.getCell(2));
                if (productName == null || productName.isEmpty())
                    productName = "";  // 상품명이 없어도 저장 (빈 문자열)

                BigDecimal quantity = getCellValueAsBigDecimal(row.getCell(3));
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                // 실매출 읽기 (컬럼 4, 쉼표 제거 후 정수로 변환)
                Integer actualSales = getCellValueAsInteger(row.getCell(4));

                // 2. 상품 조회 (POS 코드로 조회 - Product.code가 POS 코드)
                Product product = productRepository.findByCode(code)
                        .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND,
                                "Product not found with code: " + code));

                // 3. Retail 엔티티 생성
                Retail retail = Retail.builder()
                        .store(store)
                        .product(product)
                        .productCode(code)  // 판매 시점의 상품 코드
                        .productName(productName)  // 판매 시점의 상품명 (POS에서 저장된 값)
                        .soldDate(soldDate)  // 판매일자
                        .quantity(quantity)
                        .actualSales(actualSales)  // 실매출
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
                // 쉼표 제거 후 변환 (예: "3,566" -> 3566)
                return new BigDecimal(cell.getStringCellValue().replace(",", ""));
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        } else {
            return BigDecimal.ZERO;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null)
            return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                // 쉼표 제거 후 정수로 변환 (예: "100,000" -> 100000)
                String value = cell.getStringCellValue().replace(",", "").trim();
                if (value.isEmpty()) {
                    return null;
                }
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<RetailResponse> getRetailList(
            Long userId, Integer page, Integer size, LocalDate soldDate,
            LocalDate startDate, LocalDate endDate
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[RetailService] 소매 내역 목록 조회 요청 - userId: {}, storeId: {}, soldDate: {}, startDate: {}, endDate: {}",
                userId, store.getId(), soldDate, startDate, endDate);

        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "soldDate");
        Page<Retail> retailPage;

        if (soldDate != null) {
            // 특정 날짜 조회
            retailPage = retailRepository.findAllByStoreIdAndSoldDate(store.getId(), soldDate, pageable);
        } else if (startDate != null && endDate != null) {
            // 날짜 범위 조회
            retailPage = retailRepository.findAllByStoreIdAndSoldDateBetween(
                    store.getId(), startDate, endDate, pageable);
        } else {
            // 날짜 조건 없이 최근 데이터 조회 (최근 30일)
            LocalDate defaultEndDate = LocalDate.now();
            LocalDate defaultStartDate = defaultEndDate.minusDays(30);
            retailPage = retailRepository.findAllByStoreIdAndSoldDateBetween(
                    store.getId(), defaultStartDate, defaultEndDate, pageable);
        }

        Page<RetailResponse> mapped = retailPage.map(RetailResponse::from);

        log.info("[RetailService] 소매 내역 목록 조회 성공 - userId: {}, storeId: {}, totalElements: {}",
                userId, store.getId(), mapped.getTotalElements());
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public List<RetailResponse> getRetailListByDate(Long userId, LocalDate soldDate) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[RetailService] 특정 날짜 소매 내역 조회 요청 - userId: {}, storeId: {}, soldDate: {}",
                userId, store.getId(), soldDate);

        List<Retail> retails = retailRepository.findAllByStoreIdAndSoldDate(store.getId(), soldDate);
        List<RetailResponse> responses = retails.stream()
                .map(RetailResponse::from)
                .toList();

        log.info("[RetailService] 특정 날짜 소매 내역 조회 성공 - userId: {}, storeId: {}, count: {}",
                userId, store.getId(), responses.size());
        return responses;
    }
}
