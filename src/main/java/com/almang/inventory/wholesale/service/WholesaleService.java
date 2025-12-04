package com.almang.inventory.wholesale.service;

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
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.wholesale.domain.Wholesale;
import com.almang.inventory.wholesale.domain.WholesaleItem;
import com.almang.inventory.wholesale.domain.WholesaleStatus;
import com.almang.inventory.wholesale.dto.request.ConfirmWholesaleRequest;
import com.almang.inventory.wholesale.dto.request.CreatePendingWholesaleRequest;
import com.almang.inventory.wholesale.dto.request.CreateWholesaleItemRequest;
import com.almang.inventory.wholesale.dto.response.CancelWholesaleResponse;
import com.almang.inventory.wholesale.dto.response.ConfirmWholesaleResponse;
import com.almang.inventory.wholesale.dto.response.WholesaleResponse;
import com.almang.inventory.wholesale.repository.WholesaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WholesaleService {

    private final WholesaleRepository wholesaleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserContextProvider userContextProvider;

    @Transactional
    public WholesaleResponse createPendingWholesale(CreatePendingWholesaleRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[WholesaleService] 출고 대기 생성 요청 - userId: {}, storeId: {}", userId, store.getId());

        validateWholesaleItemsNotEmpty(request.items());

        List<WholesaleItem> items = createWholesaleItems(request.items(), store);
        Wholesale wholesale = toWholesaleEntity(request, store);
        items.forEach(wholesale::addItem);
        Wholesale saved = wholesaleRepository.save(wholesale);

        log.info("[WholesaleService] 출고 대기 생성 성공 - wholesaleId: {}, storeId: {}", saved.getId(), store.getId());
        return WholesaleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public WholesaleResponse getWholesale(Long wholesaleId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[WholesaleService] 출고 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        Wholesale wholesale = findWholesaleByIdAndValidateAccess(wholesaleId, store);

        log.info("[WholesaleService] 출고 조회 성공 - wholesaleId: {}", wholesale.getId());
        return WholesaleResponse.from(wholesale);
    }

    @Transactional(readOnly = true)
    public PageResponse<WholesaleResponse> getWholesaleList(
            Long userId, Integer page, Integer size, WholesaleStatus status,
            LocalDate fromDate, LocalDate toDate, String orderReference
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[WholesaleService] 출고 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());

        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "createdAt");
        Page<Wholesale> wholesalePage = findWholesalesByFilter(
                store.getId(), status, fromDate, toDate, orderReference, pageable
        );
        Page<WholesaleResponse> mapped = wholesalePage.map(WholesaleResponse::from);

        log.info("[WholesaleService] 출고 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    @Transactional
    public ConfirmWholesaleResponse confirmWholesale(Long wholesaleId, ConfirmWholesaleRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[WholesaleService] 출고 완료 처리 요청 - userId: {}, storeId: {}, wholesaleId: {}",
                userId, store.getId(), wholesaleId);

        Wholesale wholesale = findWholesaleByIdAndValidateAccess(wholesaleId, store);
        wholesale.confirm(request.releaseDate());

        // 출고 완료 후 재고 차감
        for (WholesaleItem item : wholesale.getItems()) {
            Inventory inventory = findInventoryByProductId(item.getProduct().getId());
            inventory.confirmOutgoing(item.getQuantity());
        }

        log.info("[WholesaleService] 출고 완료 처리 성공 - wholesaleId: {}", wholesale.getId());
        return new ConfirmWholesaleResponse(
                wholesale.getId(),
                true,
                wholesale.getReleaseDate()
        );
    }

    @Transactional
    public CancelWholesaleResponse cancelWholesale(Long wholesaleId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[WholesaleService] 출고 취소 요청 - userId: {}, storeId: {}, wholesaleId: {}",
                userId, store.getId(), wholesaleId);

        Wholesale wholesale = findWholesaleByIdAndValidateAccess(wholesaleId, store);
        wholesale.cancel();

        // 출고 취소 후 출고 예정 수량 차감
        for (WholesaleItem item : wholesale.getItems()) {
            Inventory inventory = findInventoryByProductId(item.getProduct().getId());
            inventory.decreaseOutgoing(item.getQuantity());
        }

        log.info("[WholesaleService] 출고 취소 성공 - wholesaleId: {}", wholesale.getId());
        return new CancelWholesaleResponse(wholesale.getId(), true);
    }

    private List<WholesaleItem> createWholesaleItems(List<CreateWholesaleItemRequest> requests, Store store) {
        List<WholesaleItem> items = new ArrayList<>();

        for (CreateWholesaleItemRequest request : requests) {
            Product product = findProductByIdAndValidateAccess(request.productId(), store);
            Inventory inventory = findInventoryByProductId(product.getId());

            // 재고 검증 (가용 재고 = 창고 재고 - 출고 예정 수량)
            BigDecimal availableStock = inventory.getAvailableStock();
            if (availableStock.compareTo(request.quantity()) < 0) {
                throw new BaseException(ErrorCode.NOT_ENOUGH_STOCK,
                        String.format("상품 '%s'의 창고 재고가 부족합니다. (요청: %s, 가용 재고: %s)",
                                product.getName(), request.quantity(), availableStock));
            }

            // 출고 예정 수량 증가
            inventory.increaseOutgoing(request.quantity());

            items.add(toWholesaleItemEntity(request, product));
        }
        return items;
    }

    private WholesaleItem toWholesaleItemEntity(CreateWholesaleItemRequest request, Product product) {
        Integer amount = null;
        if (request.unitPrice() != null && request.quantity() != null) {
            amount = request.quantity().multiply(BigDecimal.valueOf(request.unitPrice())).intValue();
        }

        return WholesaleItem.builder()
                .product(product)
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .amount(amount)
                .note(request.note())
                .build();
    }

    private Wholesale toWholesaleEntity(CreatePendingWholesaleRequest request, Store store) {
        return Wholesale.builder()
                .store(store)
                .orderReference(request.orderReference())
                .status(WholesaleStatus.PENDING)
                .releaseDate(null)
                .activated(true)
                .items(new ArrayList<>())
                .build();
    }

    private Product findProductByIdAndValidateAccess(Long productId, Store store) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }
        return product;
    }

    private Inventory findInventoryByProductId(Long productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new BaseException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private void validateWholesaleItemsNotEmpty(List<CreateWholesaleItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BaseException(ErrorCode.WHOLESALE_ITEM_EMPTY);
        }
    }

    private Wholesale findWholesaleByIdAndValidateAccess(Long wholesaleId, Store store) {
        Wholesale wholesale = wholesaleRepository.findById(wholesaleId)
                .orElseThrow(() -> new BaseException(ErrorCode.WHOLESALE_NOT_FOUND));

        if (!wholesale.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.WHOLESALE_ACCESS_DENIED);
        }
        return wholesale;
    }

    private Page<Wholesale> findWholesalesByFilter(
            Long storeId, WholesaleStatus status, LocalDate fromDate, LocalDate toDate,
            String orderReference, Pageable pageable
    ) {
        LocalDate startDate = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
        LocalDate endDate = toDate != null ? toDate : LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        boolean hasStatus = status != null;
        boolean hasOrderReference = orderReference != null && !orderReference.isBlank();

        // 1) 필터 없음
        if (!hasStatus && !hasOrderReference) {
            return wholesaleRepository.findAllByStoreIdAndCreatedAtBetween(storeId, start, end, pageable);
        }

        // 2) 상태 필터만
        if (hasStatus && !hasOrderReference) {
            return wholesaleRepository.findAllByStoreIdAndStatusAndCreatedAtBetween(storeId, status, start, end, pageable);
        }

        // 3) 주문서 참조 번호 필터만
        if (!hasStatus && hasOrderReference) {
            return wholesaleRepository.findAllByStoreIdAndOrderReferenceContainingAndCreatedAtBetween(
                    storeId, orderReference, start, end, pageable);
        }

        // 4) 상태 + 주문서 참조 번호 필터
        return wholesaleRepository.findAllByStoreIdAndStatusAndOrderReferenceContainingAndCreatedAtBetween(
                storeId, status, orderReference, start, end, pageable);
    }
}

