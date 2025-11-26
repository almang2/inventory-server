package com.almang.inventory.inventory.service;

import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.dto.request.UpdateInventoryRequest;
import com.almang.inventory.inventory.dto.response.InventoryResponse;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.store.domain.Store;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final UserContextProvider userContextProvider;

    @Transactional
    public void createInventory(Product product) {
        log.info("[InventoryService] 재고 생성 요청 - productId: {}", product.getId());
        Inventory inventory = toInventoryEntity(product);
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

    private Inventory toInventoryEntity(Product product) {
        return Inventory.builder()
                .product(product)
                .displayStock(BigDecimal.ZERO)
                .warehouseStock(BigDecimal.ZERO)
                .outgoingReserved(BigDecimal.ZERO)
                .incomingReserved(BigDecimal.ZERO)
                .reorderTriggerPoint(product.getStore().getDefaultCountCheckThreshold())
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
