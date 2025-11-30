package com.almang.inventory.customerorder.service;

import com.almang.inventory.customerorder.domain.CustomerOrder;
import com.almang.inventory.customerorder.domain.CustomerOrderItem;
import com.almang.inventory.customerorder.dto.request.CustomerOrderItemRequest;
import com.almang.inventory.customerorder.dto.request.CustomerOrderRequest;
import com.almang.inventory.customerorder.repository.CustomerOrderRepository;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository; // 기존 product 도메인의 Repository
    private final InventoryRepository inventoryRepository; // 기존 inventory 도메인의 Repository

    @Transactional
    public Long createCustomerOrderAndProcessStock(CustomerOrderRequest request) {
        // 1. 이미 존재하는 카페24 주문인지 확인 (중복 처리 방지)
        customerOrderRepository.findByCafe24OrderId(request.getCafe24OrderId())
                .ifPresent(order -> {
                    throw new BaseException(ErrorCode.DUPLICATE_CUSTOMER_ORDER,
                            "Cafe24 Order ID already exists: " + request.getCafe24OrderId());
                });

        // 2. CustomerOrder 엔티티 생성
        CustomerOrder customerOrder = CustomerOrder.builder()
                .cafe24OrderId(request.getCafe24OrderId())
                .orderAt(request.getOrderAt())
                .isPaid(request.getIsPaid().equalsIgnoreCase("T")) // 'T'/'F' 문자열을 boolean으로 변환
                .isCanceled(request.getIsCanceled().equalsIgnoreCase("T")) // 'T'/'F' 문자열을 boolean으로 변환
                .paymentMethod(request.getPaymentMethodName() != null && !request.getPaymentMethodName().isEmpty()
                        ? request.getPaymentMethodName().get(0)
                        : null)
                .paymentAmount(request.getPaymentAmount())
                .billingName(request.getBillingName())
                .memberId(request.getMemberId())
                .memberEmail(request.getMemberEmail())
                .initialOrderPriceAmount(request.getInitialOrderAmount().getOrderPriceAmount())
                .shippingFee(request.getInitialOrderAmount().getShippingFee())
                .build();

        // 3. CustomerOrderItem 처리 및 재고 처리 (감소 로직은 Placeholder)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CustomerOrderItemRequest itemRequest : request.getItems()) {
                // 3.1. 상품 조회 (cafe24Code 사용)
                Product product = productRepository.findByCafe24Code(itemRequest.getProductCode())
                        .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND,
                                "Product not found with code: " + itemRequest.getProductCode()));

                // 3.2. Inventory 조회 (상품과 연결된 재고 정보)
                // Inventory inventory = inventoryRepository.findByProduct(product)
                // .orElseThrow(() -> new BaseException(ErrorCode.INVENTORY_NOT_FOUND,
                // "Inventory not found for product: " + product.getName()));
                // TODO: 재고 조회 및 감소 로직 구현 필요

                // 3.3. 재고 감소 로직 (Placeholder)
                // ========================================================================
                // TODO: 카페24 연동 정책에 따라 정확한 재고 감소 로직을 여기에 구현해야 합니다.
                // 예: inventory.decreaseWarehouse(new BigDecimal(itemRequest.getQuantity()));
                // 현재는 재고 감소 로직이 적용되지 않습니다.
                // ========================================================================
                log.warn("카페24 주문 ID {}의 상품 {} (수량 {})에 대한 재고 감소 로직이 정의되지 않았습니다.",
                        request.getCafe24OrderId(), itemRequest.getProductName(), itemRequest.getQuantity());

                // 3.4. CustomerOrderItem 엔티티 생성 및 CustomerOrder에 추가
                CustomerOrderItem customerOrderItem = CustomerOrderItem.builder()
                        .product(product) // Product 엔티티와 연관 관계 설정
                        .productCode(itemRequest.getProductCode())
                        .productName(itemRequest.getProductName())
                        .quantity(itemRequest.getQuantity())
                        .optionValue(itemRequest.getOptionValue())
                        .variantCode(itemRequest.getVariantCode())
                        .itemCode(itemRequest.getItemCode())
                        .build();
                customerOrder.addOrderItem(customerOrderItem); // CustomerOrder에 주문 항목 추가
            }
        } else {
            log.warn("카페24 주문 ID {}에 상품 항목이 없습니다. 재고 처리가 수행되지 않습니다.", request.getCafe24OrderId());
        }

        // 4. CustomerOrder 저장
        CustomerOrder savedOrder = customerOrderRepository.save(customerOrder);
        return savedOrder.getId();
    }

    // 추가적인 고객 주문 관련 비즈니스 로직은 여기에 구현될 수 있습니다. F_TEST
}
