package com.almang.inventory.receipt.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.repository.OrderRepository;
import com.almang.inventory.receipt.domain.Receipt;
import com.almang.inventory.receipt.domain.ReceiptItem;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
import com.almang.inventory.receipt.repository.ReceiptRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
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
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReceiptResponse createReceiptFromOrder(Long orderId, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[ReceiptService] 발주 기반 입고 생성 요청 - userId: {}, storeId: {}, orderId: {}",
                user.getId(), store.getId(), orderId);

        Order order = findOrderByIdAndValidateAccess(orderId, store);
        validateOrderStatusForReceipt(order);

        Receipt receipt = toReceiptEntity(order, store);
        List<ReceiptItem> items = createReceiptItemsFromOrder(order);
        items.forEach(receipt::addItem);
        Receipt saved = receiptRepository.save(receipt);

        log.info("[ReceiptService] 발주 기반 입고 생성 성공 - userId: {}, storeId: {}, orderId: {}",
                user.getId(), store.getId(), orderId);
        return ReceiptResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptFromOrder(Long orderId, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[ReceiptService] 발주 기반 입고 조회 요청 - userId: {}, storeId: {}, orderId: {}",
                user.getId(), store.getId(), orderId);

        Order order = findOrderByIdAndValidateAccess(orderId, store);
        Receipt receipt = findReceiptByOrderId(orderId);

        log.info("[ReceiptService] 발주 기반 입고 조회 성공 - receiptId: {}", receipt.getId());
        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public ReceiptResponse getReceipt(Long receiptId, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[ReceiptService] 입고 조회 요청 - userId: {}, storeId: {}", user.getId(), store.getId());
        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);

        log.info("[ReceiptService] 입고 조회 성공 - receiptId: {}", receipt.getId());
        return ReceiptResponse.from(receipt);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReceiptResponse> getReceiptList(
            Long userId, Integer page, Integer size, Long vendorId,
            ReceiptStatus status, LocalDate fromDate, LocalDate toDate
    ) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[ReceiptService] 입고 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());

        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "createdAt");
        Page<Receipt> receiptPage = findReceiptsByFilter(store.getId(), vendorId, status, fromDate, toDate, pageable);
        Page<ReceiptResponse> mapped = receiptPage.map(ReceiptResponse::from);

        log.info("[ReceiptService] 입고 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    private List<ReceiptItem> createReceiptItemsFromOrder(Order order) {
        List<ReceiptItem> items = new ArrayList<>();

        for (OrderItem orderItem : order.getItems()) {
            ReceiptItem item = toReceiptItemEntity(orderItem);
            items.add(item);
        }
        return items;
    }

    private Receipt toReceiptEntity(Order order, Store store) {
        return Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
    }

    private ReceiptItem toReceiptItemEntity(OrderItem orderItem) {
        return ReceiptItem.builder()
                .product(orderItem.getProduct())
                .boxCount(null)
                .measuredWeight(null)
                .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                .actualQuantity(null)
                .unitPrice(orderItem.getUnitPrice())
                .amount(orderItem.getQuantity() * orderItem.getUnitPrice())
                .errorRate(null)
                .note(null)
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private Order findOrderByIdAndValidateAccess(Long orderId, Store store) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private void validateOrderStatusForReceipt(Order order) {
        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new BaseException(ErrorCode.RECEIPT_CREATION_NOT_ALLOWED_FROM_ORDER);
        }
    }

    private Receipt findReceiptByOrderId(Long orderId) {
        return receiptRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.RECEIPT_NOT_FOUND));
    }

    private Receipt findReceiptByIdAndValidateAccess(Long receiptId, Store store) {
        Receipt receipt =  receiptRepository.findById(receiptId)
                .orElseThrow(() -> new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        if (!receipt.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED);
        }
        return receipt;
    }

    private Page<Receipt> findReceiptsByFilter(
            Long storeId, Long vendorId, ReceiptStatus status,
            LocalDate fromDate, LocalDate toDate, Pageable pageable
    ) {

        LocalDate startDate = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
        LocalDateTime start = startDate.atStartOfDay();

        LocalDate endDate = toDate != null ? toDate : LocalDate.now();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        boolean hasVendor = vendorId != null;
        boolean hasStatus = status != null;

        // 1) 필터 없음
        if (!hasVendor && !hasStatus) {
            return receiptRepository.findAllByStoreIdAndReceiptDateBetween(
                    storeId, start, end, pageable
            );
        }

        // 2) 상태 필터
        if (!hasVendor) {
            return receiptRepository.findAllByStoreIdAndStatusAndReceiptDateBetween(
                    storeId, status, start, end, pageable
            );
        }

        // 3) 발주처 필터
        if (!hasStatus) {
            return receiptRepository.findAllByStoreIdAndOrderVendorIdAndReceiptDateBetween(
                    storeId, vendorId, start, end, pageable
            );
        }

        // 4) 발주처 + 상태 필터
        return receiptRepository.findAllByStoreIdAndOrderVendorIdAndStatusAndReceiptDateBetween(
                storeId, vendorId, status, start, end, pageable
        );
    }
}
