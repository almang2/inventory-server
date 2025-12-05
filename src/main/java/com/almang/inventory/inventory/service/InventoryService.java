package com.almang.inventory.inventory.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.domain.InventoryMoveDirection;
import com.almang.inventory.inventory.domain.InventoryScope;
import com.almang.inventory.inventory.dto.InitialInventoryValues;
import com.almang.inventory.inventory.dto.request.MoveInventoryRequest;
import com.almang.inventory.inventory.dto.request.UpdateInventoryRequest;
import com.almang.inventory.inventory.dto.response.InventoryResponse;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.store.domain.Store;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final UserContextProvider userContextProvider;

    @Transactional
    public void createInventory(Product product, InitialInventoryValues initialInventoryValues) {
        log.info("[InventoryService] 재고 생성 요청 - productId: {}", product.getId());
        Inventory inventory = toInventoryEntity(product, initialInventoryValues);
        inventoryRepository.save(inventory);
        log.info("[InventoryService] 재고 생성 성공 - inventoryId: {}", inventory.getId());
    }

    @Transactional
    public void increaseIncomingStockFromOrder(Product product, BigDecimal quantity) {
        log.info("[InventoryService] 발주 생성으로 입고 예정 수량 증가 요청 - productId: {}", product.getId());
        Inventory inventory = findInventoryByProductId(product.getId());
        inventory.increaseIncoming(quantity);
        log.info("[InventoryService] 발주 생성으로 입고 예정 수량 증가 성공 - inventoryId: {}", inventory.getId());
    }

    @Transactional
    public void decreaseIncomingStockFromOrder(Product product, BigDecimal quantity) {
        log.info("[InventoryService] 발주 삭제로 입고 예정 수량 감소 요청 - productId: {}", product.getId());
        Inventory inventory = findInventoryByProductId(product.getId());
        inventory.decreaseIncoming(quantity);
        log.info("[InventoryService] 발주 삭제로 입고 예정 수량 감소 성공 - inventoryId: {}", inventory.getId());
    }

    @Transactional
    public void updateIncomingStockFromOrder(Product product, BigDecimal diff) {
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        log.info("[InventoryService] 발주 수정으로 입고 예정 수량 변경 요청 - productId: {}", product.getId());
        Inventory inventory = findInventoryByProductId(product.getId());

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            inventory.increaseIncoming(diff);
            log.info("[InventoryService] 발주 수정으로 입고 예정 수량 증가 성공 - inventoryId: {}", inventory.getId());
            return;
        }
        inventory.decreaseIncoming(diff.abs());
        log.info("[InventoryService] 발주 수정으로 입고 예정 수량 감소 성공 - inventoryId: {}", inventory.getId());
    }

    @Transactional
    public void applyReceipt(Product product, BigDecimal expected, BigDecimal actual) {
        log.info("[InventoryService] 입고 이후 입고 예정 수량 감소 및 재고 수량 증가 요청 - productId: {}", product.getId());
        Inventory inventory = findInventoryByProductId(product.getId());
        inventory.confirmIncoming(expected, actual);
        log.info("[InventoryService] 입고 이후 입고 예정 수량 감소 및 재고 수량 증가 성공 - inventoryId: {}", inventory.getId());
    }

    @Transactional
    public void cancelIncomingReservation(Product product, BigDecimal quantity) {
        log.info("[InventoryService] 입고 취소로 입고 예정 수량 감소 요청 - productId: {}", product.getId());
        Inventory inventory = findInventoryByProductId(product.getId());
        inventory.decreaseIncoming(quantity);
        log.info("[InventoryService] 입고 취소로 입고 예정 수량 감소 성공 - inventoryId: {}", inventory.getId());
    }

    @Transactional
    public InventoryResponse updateInventory(Long inventoryId, UpdateInventoryRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[InventoryService] 재고 수동 수정 요청 - userId: {}, storeId: {}", userId, store.getId());
        Inventory inventory = findInventoryByIdAndValidateAccess(inventoryId, store);
        validateProductMatch(inventory, request.productId());

        inventory.updateManually(
                request.displayStock(), request.warehouseStock(), request.outgoingReserved(),
                request.incomingReserved(), request.reorderTriggerPoint()
        );

        log.info("[InventoryService] 재고 수동 수정 성공 - inventoryId: {}", inventory.getId());
        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProduct(Long productId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[InventoryService] 품목으로 재고 조회 요청 - userId: {}, productId: {}", userId, productId);
        Inventory inventory = findInventoryByProductId(productId);
        validateStoreMatch(inventory, store.getId());

        log.info("[InventoryService] 품목으로 재고 조회 성공 - inventoryId: {}", inventory.getId());
        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long inventoryId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[InventoryService] 재고 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        Inventory inventory = findInventoryByIdAndValidateAccess(inventoryId, store);

        log.info("[InventoryService] 재고 조회 성공 - inventoryId: {}", inventory.getId());
        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getStoreInventoryList(
            Long userId, int page, int size, String scopeParam, String q, String sortParam
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[InventoryService] 상점 재고 전체 조회 요청 - userId: {}, storeId: {}", userId, store.getId());

        InventoryScope scope = InventoryScope.from(scopeParam);

        Direction direction = "productName".equals(sortParam)
                ? Direction.ASC
                : Direction.DESC;

        String sortBy = "productName".equals(sortParam)
                ? "product.name"
                : "updatedAt";

        Pageable pageable = PaginationUtil.createPageRequest(page, size, direction, sortBy);

        Page<Inventory> inventoryPage = inventoryRepository.findByFilter(
                store.getId(),
                scope.name(),
                (q == null || q.isBlank()) ? null : q,
                pageable
        );
        Page<InventoryResponse> mapped = inventoryPage.map(InventoryResponse::from);

        log.info("[InventoryService] 상점 재고 전체 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    @Transactional
        public InventoryResponse moveInventory(Long inventoryId, MoveInventoryRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[InventoryService] 재고 이동 요청 - userId: {}, storeId: {}", userId, store.getId());
        Inventory inventory = findInventoryByIdAndValidateAccess(inventoryId, store);

        if (request.direction() == InventoryMoveDirection.WAREHOUSE_TO_DISPLAY) {
            inventory.moveWarehouseToDisplay(request.quantity());
            log.info("[InventoryService] 창고 재고에서 매대 재고로 이동 성공 - inventoryId: {}", inventory.getId());
        }
        if (request.direction() == InventoryMoveDirection.DISPLAY_TO_WAREHOUSE) {
            inventory.moveDisplayToWarehouse(request.quantity());
            log.info("[InventoryService] 매대 재고에서 창고 재고로 이동 성공 - inventoryId: {}", inventory.getId());
        }
        return InventoryResponse.from(inventory);
    }

    private Inventory toInventoryEntity(Product product, InitialInventoryValues initialInventoryValues) {
        return Inventory.builder()
                .product(product)
                .displayStock(initialInventoryValues.displayStock())
                .warehouseStock(initialInventoryValues.warehouseStock())
                .outgoingReserved(initialInventoryValues.outgoingReserved())
                .incomingReserved(initialInventoryValues.incomingReserved())
                .reorderTriggerPoint(initialInventoryValues.reorderTriggerPoint())
                .deletedAt(null)
                .build();
    }

    private Inventory findInventoryByProductId(Long productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new BaseException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private Inventory findInventoryByIdAndValidateAccess(Long inventoryId, Store store) {
        Inventory inventory =  inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new BaseException(ErrorCode.INVENTORY_NOT_FOUND));

        if (!inventory.getProduct().getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.INVENTORY_ACCESS_DENIED);
        }
        return inventory;
    }

    private void validateProductMatch(Inventory inventory, Long productId) {
        if (!inventory.getProduct().getId().equals(productId)) {
            throw new BaseException(ErrorCode.INVENTORY_PRODUCT_MISMATCH);
        }
    }

    private void validateStoreMatch(Inventory inventory, Long storeId) {
        if (!inventory.getProduct().getStore().getId().equals(storeId)) {
            throw new BaseException(ErrorCode.INVENTORY_ACCESS_DENIED);
        }
    }
}
