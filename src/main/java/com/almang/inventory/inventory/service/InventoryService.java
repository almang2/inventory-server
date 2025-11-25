package com.almang.inventory.inventory.service;

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
}
