package com.almang.inventory.wholesale.service;

import com.almang.inventory.global.cafe24.dto.Cafe24OrderResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.wholesale.domain.Wholesale;
import com.almang.inventory.wholesale.domain.WholesaleItem;
import com.almang.inventory.wholesale.domain.WholesaleStatus;
import com.almang.inventory.wholesale.repository.WholesaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WholesaleService {

    private final WholesaleRepository wholesaleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;

    @Value("${wholesale.default-store-id:1}")
    private Long defaultStoreId;

    /**
     * Cafe24 주문을 도매 주문으로 변환하여 저장하고 재고를 차감합니다.
     * 
     * @param cafe24Order Cafe24 주문 정보
     * @return 생성된 도매 주문
     */
    @Transactional
    public Wholesale createWholesaleFromCafe24Order(Cafe24OrderResponse.Order cafe24Order) {
        log.info("Cafe24 주문을 도매 주문으로 변환 시작 - orderId: {}", cafe24Order.getOrderId());

        // 중복 체크 및 입금 완료 처리
        Optional<Wholesale> existingWholesale = wholesaleRepository.findByCafe24OrderId(cafe24Order.getOrderId());
        if (existingWholesale.isPresent()) {
            Wholesale existing = existingWholesale.get();
            
            // 입금 완료 처리: paid="F" → "T"로 변경되었고, 아직 재고 차감 안 했으면 차감
            if ("T".equals(cafe24Order.getPaid()) && existing.getStatus() == WholesaleStatus.PAYMENT_PENDING) {
                log.info("입금 완료 처리 - orderId: {}, 재고 차감 시작", cafe24Order.getOrderId());
                
                // 상태 업데이트
                existing.updateStatus(WholesaleStatus.ORDER_CONFIRMED);
                
                // 재고 차감 (아직 차감하지 않은 경우)
                for (WholesaleItem item : existing.getItems()) {
                    try {
                        deductInventory(item.getProduct(), BigDecimal.valueOf(item.getQuantity()));
                    } catch (Exception e) {
                        log.error("재고 차감 실패 - productId: {}, quantity: {}", 
                                item.getProduct().getId(), item.getQuantity(), e);
                    }
                }
                
                log.info("입금 완료 처리 및 재고 차감 완료 - orderId: {}", cafe24Order.getOrderId());
            }
            
            return existing;
        }

        // Store 찾기 (기본 Store 또는 첫 번째 Store)
        Store store = findDefaultStore();
        
        log.info("Store 찾기 완료 - storeId: {}", store.getId());

        // 주문 상태 결정
        WholesaleStatus status = determineWholesaleStatus(cafe24Order);

        // Wholesale 생성
        Wholesale wholesale = Wholesale.builder()
                .store(store)
                .status(status)
                .cafe24OrderId(cafe24Order.getOrderId())
                .build();

        // WholesaleItem 생성 및 재고 차감
        List<WholesaleItem> items = new ArrayList<>();
        if (cafe24Order.getItems() != null && !cafe24Order.getItems().isEmpty()) {
            for (Cafe24OrderResponse.OrderItem cafe24Item : cafe24Order.getItems()) {
                WholesaleItem item = createWholesaleItem(wholesale, cafe24Item, store);
                if (item != null) {
                    items.add(item);
                    // 재고 차감 (결제 완료된 경우만)
                    if ("T".equals(cafe24Order.getPaid())) {
                        deductInventory(item.getProduct(), BigDecimal.valueOf(item.getQuantity()));
                    }
                }
            }
        }

        // Wholesale 저장
        items.forEach(wholesale::addItem);
        Wholesale saved = wholesaleRepository.save(wholesale);

        log.info("Cafe24 주문을 도매 주문으로 변환 완료 - wholesaleId: {}, orderId: {}", 
                saved.getId(), cafe24Order.getOrderId());

        return saved;
    }

    private Store findDefaultStore() {
        if (defaultStoreId != null) {
            return storeRepository.findById(defaultStoreId)
                    .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));
        }

        // 기본 Store가 없으면 첫 번째 Store 사용
        return storeRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));
    }

    private WholesaleStatus determineWholesaleStatus(Cafe24OrderResponse.Order cafe24Order) {
        // 결제 여부에 따라 상태 결정
        if ("T".equals(cafe24Order.getPaid())) {
            return WholesaleStatus.ORDER_CONFIRMED;
        } else {
            return WholesaleStatus.PAYMENT_PENDING;
        }
    }

    private WholesaleItem createWholesaleItem(Wholesale wholesale, 
                                             Cafe24OrderResponse.OrderItem cafe24Item, 
                                             Store store) {
        // Product 찾기 (product_code로)
        Optional<Product> productOpt = productRepository.findByCode(cafe24Item.getProductCode());
        if (productOpt.isEmpty()) {
            log.warn("상품을 찾을 수 없음 - productCode: {}", cafe24Item.getProductCode());
            return null;
        }

        Product product = productOpt.get();
        
        // Store 일치 확인
        if (!product.getStore().getId().equals(store.getId())) {
            log.warn("상품의 Store가 일치하지 않음 - productStoreId: {}, requestStoreId: {}", 
                    product.getStore().getId(), store.getId());
            return null;
        }

        // 가격 결정 (도매가 우선, 없으면 소매가)
        int unitPrice = product.getWholesalePrice() > 0 
                ? product.getWholesalePrice() 
                : product.getRetailPrice();

        // Cafe24 가격이 있으면 사용
        if (cafe24Item.getPrice() != null) {
            try {
                String priceStr = cafe24Item.getPrice().toString();
                unitPrice = (int) Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                log.warn("Cafe24 가격 파싱 실패 - price: {}", cafe24Item.getPrice());
            }
        }

        int quantity = cafe24Item.getQuantity() != null ? cafe24Item.getQuantity() : 0;
        int amount = quantity * unitPrice;

        return WholesaleItem.builder()
                .wholesale(wholesale)
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .amount(amount)
                .build();
    }

    private void deductInventory(Product product, BigDecimal quantity) {
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.INVENTORY_NOT_FOUND));

        try {
            inventory.decreaseDisplay(quantity);
            log.info("재고 차감 완료 - productId: {}, quantity: {}", product.getId(), quantity);
        } catch (BaseException e) {
            log.error("재고 차감 실패 - productId: {}, quantity: {}, error: {}", 
                    product.getId(), quantity, e.getMessage());
            throw e;
        }
    }
}

