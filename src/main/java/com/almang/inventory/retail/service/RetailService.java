package com.almang.inventory.retail.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.retail.domain.Retail;
import com.almang.inventory.retail.dto.excel.RetailExcelRowDto;
import com.almang.inventory.retail.dto.response.RetailResponse;
import com.almang.inventory.retail.parser.RetailExcelParser;
import com.almang.inventory.retail.repository.RetailRepository;
import com.almang.inventory.store.domain.Store;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final RetailRepository retailRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserContextProvider userContextProvider;
    private final RetailExcelParser retailExcelParser;

    @Transactional
    public RetailUploadResult processRetailExcel(MultipartFile file, Long userId) {
        // 1. 상점 조회
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        // 2. soldDate 계산
        LocalDate soldDate = LocalDate.now(SEOUL_ZONE);

        // 3. 기존 데이터 soft delete
        List<Retail> existingRetails = retailRepository.findAllByStoreIdAndSoldDate(store.getId(), soldDate);
        if (!existingRetails.isEmpty()) {
            log.warn("[RetailService] 해당 날짜({})에 이미 소매 데이터가 존재합니다. 기존 데이터를 소프트 삭제하고 새로 저장합니다. - storeId: {}, count: {}",
                    soldDate, store.getId(), existingRetails.size());
            // 삭제 전 상세 정보 로그 기록 (복구를 위한 참고용)
            existingRetails.forEach(retail ->
                    log.debug("[RetailService] 소프트 삭제 대상 - retailId: {}, productCode: {}, quantity: {}, actualSales: {}",
                            retail.getId(), retail.getProductCode(), retail.getQuantity(), retail.getActualSales())
            );
            // 소프트 삭제: deletedAt 필드 설정
            existingRetails.forEach(Retail::delete);
            retailRepository.saveAll(existingRetails);
        }

        List<RetailExcelRowDto> rows;
        try (InputStream inputStream = file.getInputStream()) {
            rows = retailExcelParser.parse(inputStream);
        } catch (IOException e) {
            throw new BaseException(ErrorCode.EXCEL_PARSE_ERROR);
        }

        List<Retail> retails = new ArrayList<>();
        List<String> skippedProducts = new ArrayList<>();

        for (RetailExcelRowDto row : rows) {
            String code = row.code();
            String productName = row.productName();
            BigDecimal quantity = row.quantity();
            Integer actualSales = row.actualSales();

            Product product = productRepository.findByCode(code).orElse(null);
            if (product == null) {
                String skippedInfo = String.format("%s (%s)", code, productName);
                skippedProducts.add(skippedInfo);
                log.warn("[RetailService] 상품을 찾을 수 없어 스킵합니다 - rowIndex: {}, code: {}, productName: {}",
                        row.rowIndex(), code, productName);
                continue;  // 상품이 없으면 해당 행 스킵하고 계속 진행
            }

            // 품목 생성 시 자동으로 재고 레코드가 생성되므로, 재고 레코드가 없는 경우는 매우 드뭅니다
            // 재고 차감 시 마이너스 방지 검증(decreaseDisplay)이 있으므로, 재고 레코드가 없으면 스킵
            var inventoryOpt = inventoryRepository.findByProduct(product);
            if (inventoryOpt.isEmpty()) {
                String skippedInfo = String.format("%s (%s) - 재고 레코드 없음", code, productName);
                skippedProducts.add(skippedInfo);
                log.warn("[RetailService] 재고 레코드가 없어 스킵합니다 - productId: {}, productCode: {}, productName: {}",
                        product.getId(), code, productName);
                continue;  // 재고 레코드가 없으면 해당 행 스킵하고 계속 진행
            }

            // 재고 차감을 먼저 시도 (성공한 경우에만 Retail 엔티티 생성)
            // 재고 부족 시 예외를 catch하여 해당 상품만 스킵하고 나머지는 계속 처리
            Inventory inventory = inventoryOpt.get();
            try {
                inventory.decreaseDisplay(quantity);
            } catch (BaseException e) {
                // 재고 부족 시 해당 상품을 스킵하고 계속 진행
                // decreaseDisplay() 메서드는 DISPLAY_STOCK_NOT_ENOUGH 예외를 던짐
                BigDecimal currentStock = inventory.getDisplayStock();
                String skippedInfo = String.format("%s (%s) - 재고 부족 (필요: %s, 현재: %s)",
                        code, productName, quantity, currentStock);
                skippedProducts.add(skippedInfo);
                log.warn("[RetailService] 재고 부족으로 스킵합니다 - productCode: {}, productName: {}, required: {}, available: {}",
                        code, productName, quantity, currentStock);
                continue;  // 재고 부족이면 해당 행 스킵하고 계속 진행
            }

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
        }

        // 5. Retail 저장
        retailRepository.saveAll(retails);

        return new RetailUploadResult(retails.size(), skippedProducts);
    }

    // 업로드 결과를 담는 내부 클래스
    public record RetailUploadResult(
            int processedCount,  // 처리된 상품 수
            List<String> skippedProducts  // 스킵된 상품 목록 (코드 + 상품명)
    ) {}

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
            // Asia/Seoul 타임존을 명시적으로 사용하여 서버 타임존과 무관하게 일관된 날짜 계산
            LocalDate defaultEndDate = LocalDate.now(SEOUL_ZONE);
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
