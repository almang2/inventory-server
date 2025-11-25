package com.almang.inventory.inventory.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
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
            log.info("[InventoryService] 발주 수정으로 입고 예정 수량 증가 성공 - productId: {}", inventory.getId());
            return;
        }
        inventory.decreaseIncoming(diff.abs());
        log.info("[InventoryService] 발주 수정으로 입고 예정 수량 감소 성공 - productId: {}", inventory.getId());
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
}
