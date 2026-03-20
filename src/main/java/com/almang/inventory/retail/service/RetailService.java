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
import com.almang.inventory.retail.dto.upload.RetailUploadResult;
import com.almang.inventory.retail.dto.excel.RetailExcelRowDto;
import com.almang.inventory.retail.dto.upload.SkipReason;
import com.almang.inventory.retail.dto.upload.SkippedRow;
import com.almang.inventory.retail.dto.response.RetailResponse;
import com.almang.inventory.retail.dto.upload.UploadPreparationResult;
import com.almang.inventory.retail.parser.RetailExcelParser;
import com.almang.inventory.retail.repository.RetailRepository;
import com.almang.inventory.store.domain.Store;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final RetailUploadTxService retailUploadTxService;

    public RetailUploadResult processRetailExcel(MultipartFile file, Long userId) {
        // 상점 조회
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        // soldDate 계산
        LocalDate soldDate = LocalDate.now(SEOUL_ZONE);

        UploadPreparationResult ctx = prepareUploadContext(file, store);

        if (!ctx.rows().isEmpty() && ctx.rows().size() == ctx.skippedRows().size()) {
            return new RetailUploadResult(0, ctx.skippedRows());
        }

        RetailUploadResult uploadResult = retailUploadTxService.applyUploadChanges(ctx, store, soldDate);

        Map<SkipReason, Long> skipReasonCounts = uploadResult.skippedRows().stream()
                .collect(Collectors.groupingBy(SkippedRow::reason, Collectors.counting()));
        log.info(
                "[RetailService] 업로드 처리 완료 - storeId: {}, soldDate: {}, totalRows: {}, processedCount: {}, skippedCount: {}, skipReasonCounts: {}",
                store.getId(),
                soldDate,
                ctx.rows().size(),
                uploadResult.processedCount(),
                uploadResult.skippedRows().size(),
                skipReasonCounts
        );

        return uploadResult;
    }

    private UploadPreparationResult prepareUploadContext(MultipartFile file, Store store) {
        List<RetailExcelRowDto> rows;
        List<SkippedRow> skippedRows = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            rows = retailExcelParser.parse(inputStream);
        } catch (IOException e) {
            throw new BaseException(ErrorCode.EXCEL_PARSE_ERROR);
        }

        Set<String> productCodes = rows.stream()
                .map(RetailExcelRowDto::code)
                .collect(Collectors.toSet());
        if (productCodes.isEmpty()) {
            return new UploadPreparationResult(
                    rows, skippedRows, Map.of(), Map.of()
            );
        }

        List<Product> products = productRepository.findByStoreIdAndCodeIn(store.getId(), productCodes);
        Map<String, Product> productByCode = products.stream()
                .collect(Collectors.toMap(
                        Product::getCode,
                        p -> p,
                        (existing, ignored) -> existing
                ));

        List<Long> productIds = products.stream().map(Product::getId).toList();
        if (productIds.isEmpty()) {
            List<SkippedRow> allSkipped = rows.stream()
                    .map(r -> SkippedRow.of(
                            r.rowIndex(),
                            r.code(),
                            SkipReason.PRODUCT_NOT_FOUND
                    )).toList();
            return new UploadPreparationResult(
                    rows, allSkipped, productByCode, Map.of()
            );
        }

        List<Inventory> inventories = inventoryRepository.findAllByProduct_IdIn(productIds);
        Map<Long, Inventory> inventoryByProductId = inventories.stream()
                .collect(Collectors.toMap(
                        i -> i.getProduct().getId(), i -> i
                ));

        return new UploadPreparationResult(
                rows, skippedRows, productByCode, inventoryByProductId
        );
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
