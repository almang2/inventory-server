package com.almang.inventory.retail.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.retail.domain.Retail;
import com.almang.inventory.retail.dto.excel.RetailExcelRowDto;
import com.almang.inventory.retail.dto.upload.RetailUploadResult;
import com.almang.inventory.retail.dto.upload.SkipReason;
import com.almang.inventory.retail.dto.upload.SkippedRow;
import com.almang.inventory.retail.dto.upload.UploadPreparationResult;
import com.almang.inventory.retail.repository.RetailRepository;
import com.almang.inventory.store.domain.Store;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailUploadTxService {

    private final RetailRepository retailRepository;

    @Transactional
    public RetailUploadResult applyUploadChanges(UploadPreparationResult ctx, Store store, LocalDate soldDate) {
        return processRows(ctx, store, soldDate);
    }

    private void softDeleteExistingRetails(Store store, LocalDate soldDate) {
        // 기존 데이터 soft delete
        List<Retail> existingRetails = retailRepository.findAllByStoreIdAndSoldDate(store.getId(), soldDate);
        if (!existingRetails.isEmpty()) {
            log.warn("[RetailUploadTxService] 해당 날짜({})에 이미 소매 데이터가 존재합니다. 기존 데이터를 소프트 삭제하고 새로 저장합니다. - storeId: {}, count: {}",
                    soldDate, store.getId(), existingRetails.size());
            // 삭제 전 상세 정보 로그 기록 (복구를 위한 참고용)
            existingRetails.forEach(retail ->
                    log.debug("[RetailUploadTxService] 소프트 삭제 대상 - retailId: {}, productCode: {}, quantity: {}, actualSales: {}",
                            retail.getId(), retail.getProductCode(), retail.getQuantity(), retail.getActualSales())
            );
            // 소프트 삭제: deletedAt 필드 설정
            existingRetails.forEach(Retail::delete);
            retailRepository.saveAll(existingRetails);
        }
    }

    private RetailUploadResult processRows(UploadPreparationResult ctx, Store store, LocalDate soldDate) {
        List<Retail> retails = new ArrayList<>();
        List<SkippedRow> skippedRows = new ArrayList<>(ctx.skippedRows());

        for (RetailExcelRowDto row : ctx.rows()) {
            processSingleRow(ctx, store, soldDate, skippedRows, row)
                    .ifPresent(retails::add);
        }

        if (retails.isEmpty()) {
            log.info(
                    "[RetailUploadTxService] 유효한 업로드 행이 없어 기존 데이터를 유지합니다. - storeId: {}, soldDate: {}, skippedCount: {}",
                    store.getId(),
                    soldDate,
                    skippedRows.size()
            );
            return new RetailUploadResult(0, skippedRows);
        }

        softDeleteExistingRetails(store, soldDate);

        // Retail 저장
        retailRepository.saveAll(retails);

        return new RetailUploadResult(retails.size(), skippedRows);
    }

    private Optional<Retail> processSingleRow(
            UploadPreparationResult ctx, Store store, LocalDate soldDate,
            List<SkippedRow> skippedRows, RetailExcelRowDto row
    ) {
        String code = row.code();
        BigDecimal quantity = row.quantity();

        Product product = findProductOrSkip(ctx, skippedRows, row, code);
        if (product == null) {
            return Optional.empty();
        }

        // 재고 차감 시 마이너스 방지 검증(decreaseDisplay)이 있으므로, 재고 레코드가 없으면 스킵
        Inventory inventory = findInventoryOrSkip(ctx, skippedRows, row, code, product);
        if (inventory == null) {
            return Optional.empty();
        }

        // 재고 차감을 먼저 시도 (성공한 경우에만 Retail 엔티티 생성)
        if (!tryDecreaseOrSkip(skippedRows, row, code, quantity, inventory)) {
            return Optional.empty();
        }

        return Optional.of(createRetail(store, product, row, soldDate));
    }

    private Product findProductOrSkip(
            UploadPreparationResult ctx, List<SkippedRow> skippedRows,
            RetailExcelRowDto row, String code
    ) {
        Product product = ctx.productByCode().get(code);
        if (product == null) {
            addSkip(skippedRows, SkippedRow.of(
                    row.rowIndex(), code, SkipReason.PRODUCT_NOT_FOUND
            ));
        }
        return product;
    }

    private Inventory findInventoryOrSkip(
            UploadPreparationResult ctx, List<SkippedRow> skippedRows,
            RetailExcelRowDto row, String code, Product product
    ) {
        Inventory inventory = ctx.inventoryByProductId().get(product.getId());
        if (inventory == null) {
            addSkip(skippedRows, SkippedRow.of(
                    row.rowIndex(), code, SkipReason.INVENTORY_NOT_FOUND
            ));
        }
        return inventory;
    }

    private boolean tryDecreaseOrSkip(
            List<SkippedRow> skippedRows, RetailExcelRowDto row, String code,
            BigDecimal quantity, Inventory inventory
    ) {
        try {
            inventory.decreaseDisplay(quantity);
            return true;
        } catch (BaseException e) {
            // 재고 부족 시 해당 상품을 스킵하고 계속 진행
            // decreaseDisplay() 메서드는 DISPLAY_STOCK_NOT_ENOUGH 예외를 던짐
            if (e.getErrorCode() == ErrorCode.DISPLAY_STOCK_NOT_ENOUGH) {
                BigDecimal currentStock = inventory.getDisplayStock();
                String detail = String.format("재고 부족 (필요: %s, 현재: %s)", quantity, currentStock);
                addSkip(skippedRows, SkippedRow.of(
                        row.rowIndex(), code, SkipReason.INSUFFICIENT_STOCK, detail
                ));
                return false;
            }
            log.error(
                    "[RetailUploadTxService] 재고 차감 중 예외 발생 - rowIndex: {}, code: {}, errorCode: {}",
                    row.rowIndex(),
                    code,
                    e.getErrorCode(),
                    e
            );
            throw e;
        }
    }

    private Retail createRetail(
            Store store, Product product, RetailExcelRowDto row, LocalDate soldDate
    ) {
        return Retail.builder()
                .store(store)
                .product(product)
                .productCode(row.code()) // 판매 시점의 상품 코드
                .productName(row.productName()) // 판매 시점의 상품명 (POS에서 저장된 값)
                .soldDate(soldDate) // 판매일자
                .quantity(row.quantity())
                .actualSales(row.actualSales()) // 실매출
                .build();
    }

    private void addSkip(
            List<SkippedRow> skippedRows, SkippedRow skippedRow
    ) {
        skippedRows.add(skippedRow);
        log.warn("[RetailUploadTxService] 업로드 스킵 - rowIndex: {}, code: {}, reason: {}, message: {}",
                skippedRow.rowIndex(), skippedRow.code(), skippedRow.reason(), skippedRow.message());
    }
}
