package com.almang.inventory.order.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.dto.request.CreateOrderItemRequest;
import com.almang.inventory.order.dto.request.CreateOrderRequest;
import com.almang.inventory.order.dto.response.OrderResponse;
import com.almang.inventory.order.repository.OrderItemRepository;
import com.almang.inventory.order.repository.OrderRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.repository.VendorRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 생성 요청 - userId: {}, storeId: {}", user.getId(), store.getId());

        Vendor vendor = findVendorByIdAndValidateStore(request.vendorId(), store);

        validateOrderItemsNotEmpty(request.orderItems());
        List<OrderItem> items = createOrderItems(request.orderItems(), store);

        Order order = toOrderEntity(request, store, vendor, items);
        items.forEach(order::addItem);
        Order saved = orderRepository.save(order);

        log.info("[OrderService] 발주 생성 성공 - orderId: {}, storeId: {}, vendorId: {}",
                saved.getId(), store.getId(), vendor.getId());
        return OrderResponse.of(saved, items);
    }

    private List<OrderItem> createOrderItems(List<CreateOrderItemRequest> requests, Store store) {
        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderItemRequest request : requests) {
            Product product = findProductByIdAndValidateAccess(request.productId(), store);
            items.add(toOrderItemEntity(request, product));
        }
        return items;
    }

    private OrderItem toOrderItemEntity(CreateOrderItemRequest request, Product product) {
        return OrderItem.builder()
                .product(product)
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .amount(request.quantity() * request.unitPrice())
                .note(request.note())
                .build();
    }

    private Order toOrderEntity(
            CreateOrderRequest request, Store store, Vendor vendor, List<OrderItem> items
    ) {
        return Order.builder()
                .store(store)
                .vendor(vendor)
                .status(OrderStatus.REQUEST)
                .orderMessage(request.orderMessage())
                .leadTime(request.leadTime())
                .expectedArrival(calculateExpectedArrival(request.leadTime()))
                .activated(true)
                .totalPrice(calculateTotalPrice(items))
                .build();
    }

    private int calculateTotalPrice(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToInt(OrderItem::getAmount)
                .sum();
    }

    private LocalDate calculateExpectedArrival(Integer leadTime) {
        if (leadTime == null) {
            return null;
        }
        return LocalDate.now().plusDays(leadTime);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private Vendor findVendorByIdAndValidateStore(Long vendorId, Store store) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        if (!vendor.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.VENDOR_ACCESS_DENIED);
        }
        return vendor;
    }

    private Product findProductByIdAndValidateAccess(Long productId, Store store) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }
        return product;
    }


    private void validateOrderItemsNotEmpty(List<CreateOrderItemRequest> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BaseException(ErrorCode.ORDER_ITEM_EMPTY);
        }
    }
}
