package com.almang.inventory.order.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.inventory.service.InventoryService;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.dto.request.CreateOrderItemRequest;
import com.almang.inventory.order.dto.request.CreateOrderRequest;
import com.almang.inventory.order.dto.request.UpdateOrderItemRequest;
import com.almang.inventory.order.dto.request.UpdateOrderRequest;
import com.almang.inventory.order.dto.response.DeleteOrderResponse;
import com.almang.inventory.order.dto.response.OrderItemResponse;
import com.almang.inventory.order.dto.response.OrderResponse;
import com.almang.inventory.order.repository.OrderItemRepository;
import com.almang.inventory.order.repository.OrderRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.repository.VendorRepository;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;

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

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 조회 요청 - userId: {}, storeId: {}", user.getId(), store.getId());
        Order order = findOrderByIdAndValidateAccess(orderId, store);

        log.info("[OrderService] 발주 조회 성공 - orderId: {}", order.getId());
        return OrderResponse.of(order, order.getItems());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrderList(
            Long userId, Long vendorId, Integer page, Integer size,
            OrderStatus status, LocalDate fromDate, LocalDate toDate
    ) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "createdAt");
        Page<Order> orderPage = findOrdersByFilter(store.getId(), vendorId, status, fromDate, toDate, pageable);
        Page<OrderResponse> mapped = orderPage.map(order -> OrderResponse.of(order, order.getItems()));

        log.info("[OrderService] 발주 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest request, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 수정 요청 - userId: {}, storeId: {}", userId, store.getId());
        Order order = findOrderByIdAndValidateAccess(orderId, store);

        // 발주 업데이트 유효성 검증
        order.validateVendorNotChanged(request.vendorId());

        // 발주 정보 업데이트
        updateOrderBasicInfo(order, request);

        // 발주 상세 항목 업데이트
        updateOrderItems(order, request);

        log.info("[OrderService] 발주 수정 성공 - orderId: {}", order.getId());
        return OrderResponse.of(order, order.getItems());
    }

    @Transactional(readOnly = true)
    public OrderItemResponse getOrderItem(Long orderItemId, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 상세 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        OrderItem orderItem = findOrderItemById(orderItemId);
        validateOrderItemAccess(orderItem, store);

        log.info("[OrderService] 발주 상세 조회 성공 - orderItemId: {}", orderItem.getId());
        return OrderItemResponse.from(orderItem);
    }

    @Transactional
    public OrderItemResponse updateOrderItem(Long orderItemId, UpdateOrderItemRequest request, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 아이템 수정 요청 - userId: {}, storeId: {}", userId, store.getId());
        OrderItem orderItem = findOrderItemById(orderItemId);
        validateOrderItemAccess(orderItem, store);
        int beforeQuantity = orderItem.getQuantity();

        orderItem.update(request.quantity(), request.unitPrice(), request.note());
        int afterQuantity = orderItem.getQuantity();

        int diff = afterQuantity - beforeQuantity;
        inventoryService.updateIncomingStockFromOrder(orderItem.getProduct(), BigDecimal.valueOf(diff));

        Order order = orderItem.getOrder();
        order.updateTotalPrice(calculateTotalPrice(order.getItems()));

        log.info("[OrderService] 발주 아이템 수정 성공 - orderItemId: {}", orderItem.getId());
        return OrderItemResponse.from(orderItem);
    }

    @Transactional
    public DeleteOrderResponse deleteOrder(Long orderId, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[OrderService] 발주 삭제 요청 - userId: {}, storeId: {}", userId, store.getId());
        Order order = findOrderByIdAndValidateAccess(orderId, store);
        order.cancel();

        // 발주 취소로 인한 입고 예정 재고 차감
        for (OrderItem item : order.getItems()) {
            inventoryService.decreaseIncomingStockFromOrder(item.getProduct(), BigDecimal.valueOf(item.getQuantity()));
        }

        log.info("[OrderService] 발주 삭제 성공 - userId: {}, storeId: {}", userId, store.getId());
        return new DeleteOrderResponse(true);
    }

    private List<OrderItem> createOrderItems(List<CreateOrderItemRequest> requests, Store store) {
        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderItemRequest request : requests) {
            Product product = findProductByIdAndValidateAccess(request.productId(), store);
            items.add(toOrderItemEntity(request, product));
            inventoryService.increaseIncomingStockFromOrder(product, BigDecimal.valueOf(request.quantity()));
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

    private Order findOrderByIdAndValidateAccess(Long orderId, Store store) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private OrderItem findOrderItemByIdAndValidateAccess(Long orderItemId, Order order) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (!orderItem.getOrder().getId().equals(order.getId())) {
            throw new BaseException(ErrorCode.ORDER_ITEM_ACCESS_DENIED);
        }
        return orderItem;
    }

    private Page<Order> findOrdersByFilter(
            Long storeId, Long vendorId, OrderStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable
    ) {
        LocalDate startDate = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
        LocalDateTime start = startDate.atStartOfDay();

        LocalDate endDate = toDate != null ? toDate : LocalDate.now();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        boolean hasVendor = vendorId != null;
        boolean hasStatus = status != null;

        // 1) 필터 없음
        if (!hasVendor && !hasStatus) {
            return orderRepository.findAllByStoreIdAndCreatedAtBetween(
                    storeId, start, end, pageable
            );
        }

        // 2) 상태 필터
        if (!hasVendor) {
            return orderRepository.findAllByStoreIdAndStatusAndCreatedAtBetween(
                    storeId, status, start, end, pageable
            );
        }

        // 3) 발주처 필터
        if (!hasStatus) {
            return orderRepository.findAllByStoreIdAndVendorIdAndCreatedAtBetween(
                    storeId, vendorId, start, end, pageable
            );
        }

        // 4) 발주처 + 상태 필터
        return orderRepository.findAllByStoreIdAndVendorIdAndStatusAndCreatedAtBetween(
                storeId, vendorId, status, start, end, pageable
        );
    }

    private void updateOrderBasicInfo(Order order, UpdateOrderRequest request) {
        order.updateStatus(request.orderStatus());
        order.updateMessageAndActivated(request.orderMessage(), request.activated());
        order.updateSchedule(request.leadTime(), request.quoteReceivedAt(), request.depositConfirmedAt());
    }

    private void updateOrderItems(Order order, UpdateOrderRequest request) {
        if (request.orderItems() == null) {
            return;
        }

        for (UpdateOrderItemRequest orderItemRequest : request.orderItems()) {
            OrderItem orderItem = findOrderItemByIdAndValidateAccess(orderItemRequest.orderItemId(), order);
            int beforeQuantity = orderItem.getQuantity();
            orderItem.update(orderItemRequest.quantity(), orderItemRequest.unitPrice(), orderItemRequest.note());
            int afterQuantity = orderItem.getQuantity();

            int diff = afterQuantity - beforeQuantity;
            inventoryService.updateIncomingStockFromOrder(orderItem.getProduct(), BigDecimal.valueOf(diff));
        }
        order.updateTotalPrice(calculateTotalPrice(order.getItems()));
    }

    private OrderItem findOrderItemById(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    private void validateOrderItemAccess(OrderItem orderItem, Store store) {
        if (!orderItem.getOrder().getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.ORDER_ITEM_ACCESS_DENIED);
        }
    }
}
