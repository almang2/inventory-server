package com.almang.inventory.receipt.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.inventory.service.InventoryService;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.repository.OrderRepository;
import com.almang.inventory.receipt.domain.Receipt;
import com.almang.inventory.receipt.domain.ReceiptItem;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import com.almang.inventory.receipt.dto.request.UpdateReceiptItemRequest;
import com.almang.inventory.receipt.dto.request.UpdateReceiptRequest;
import com.almang.inventory.receipt.dto.response.ConfirmReceiptResponse;
import com.almang.inventory.receipt.dto.response.DeleteReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.DeleteReceiptResponse;
import com.almang.inventory.receipt.dto.response.ReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
import com.almang.inventory.receipt.repository.ReceiptItemRepository;
import com.almang.inventory.receipt.repository.ReceiptRepository;
import com.almang.inventory.store.domain.Store;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public class ReceiptService {

    private final InventoryService inventoryService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final OrderRepository orderRepository;
    private final UserContextProvider userContextProvider;

    @Transactional
    public ReceiptResponse createReceiptFromOrder(Long orderId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 발주 기반 입고 생성 요청 - userId: {}, storeId: {}, orderId: {}",
                userId, store.getId(), orderId);

        Order order = findOrderByIdAndValidateAccess(orderId, store);
        validateOrderStatusForReceipt(order);

        Receipt receipt = toReceiptEntity(order, store);
        List<ReceiptItem> items = createReceiptItemsFromOrder(order);
        items.forEach(receipt::addItem);
        Receipt saved = receiptRepository.save(receipt);

        log.info("[ReceiptService] 발주 기반 입고 생성 성공 - userId: {}, storeId: {}, orderId: {}",
                userId, store.getId(), orderId);
        return ReceiptResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptFromOrder(Long orderId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 발주 기반 입고 조회 요청 - userId: {}, storeId: {}, orderId: {}",
                userId, store.getId(), orderId);

        Order order = findOrderByIdAndValidateAccess(orderId, store);
        Receipt receipt = findReceiptByOrderId(orderId);

        log.info("[ReceiptService] 발주 기반 입고 조회 성공 - receiptId: {}", receipt.getId());
        return ReceiptResponse.from(receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long receiptId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);

        log.info("[ReceiptService] 입고 조회 성공 - receiptId: {}", receipt.getId());
        return ReceiptResponse.from(receipt);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReceiptResponse> getReceiptList(
            Long userId, Integer page, Integer size, Long vendorId,
            ReceiptStatus status, LocalDate fromDate, LocalDate toDate
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());

        PageRequest pageable = PaginationUtil.createPageRequest(page, size, Direction.DESC, "createdAt");
        Page<Receipt> receiptPage = findReceiptsByFilter(store.getId(), vendorId, status, fromDate, toDate, pageable);
        Page<ReceiptResponse> mapped = receiptPage.map(ReceiptResponse::from);

        log.info("[ReceiptService] 입고 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    @Transactional
    public ReceiptResponse updateReceipt(Long receiptId, UpdateReceiptRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 수정 요청 - userId: {}, storeId: {}", userId, store.getId());
        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);

        // 입고 업데이트 유효성 검증
        validateReceiptOrderNotChanged(receipt, request.orderId());

        // 입고 정보 업데이트
        receipt.update(request.status(), request.activated());

        // 입고 상세 항목 업데이트
        updateReceiptItems(receipt, request);

        log.info("[ReceiptService] 입고 수정 성공 - receiptId: {}", receipt.getId());
        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public DeleteReceiptResponse deleteReceipt(Long receiptId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 삭제 요청 - userId: {}, storeId: {}", userId, store.getId());
        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);
        receipt.delete();

        // 입고 취소 후 재고 상태 변경
        for (OrderItem orderItem : receipt.getOrder().getItems()) {
            inventoryService.cancelIncomingReservation(orderItem.getProduct(), BigDecimal.valueOf(orderItem.getQuantity()));
        }

        log.info("[ReceiptService] 입고 삭제 성공 - receiptId: {}", receipt.getId());
        return new DeleteReceiptResponse(true);
    }

    @Transactional
    public ConfirmReceiptResponse confirmReceipt(Long receiptId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 확정 요청 - userId: {}, storeId: {}", userId, store.getId());
        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);
        receipt.confirm();

        // 입고 확정 후 재고 상태 변경
        for (ReceiptItem receiptItem : receipt.getItems()) {
            int expected = receiptItem.getExpectedQuantity();
            int actual = receiptItem.getActualQuantity() != null ? receiptItem.getActualQuantity() : expected;

            inventoryService.applyReceipt(receiptItem.getProduct(), BigDecimal.valueOf(expected), BigDecimal.valueOf(actual));
        }

        log.info("[ReceiptService] 입고 확정 성공 - receiptId: {}", receipt.getId());
        return new ConfirmReceiptResponse(true);
    }

    @Transactional(readOnly = true)
    public ReceiptItemResponse getReceiptItem(Long receiptId, Long receiptItemId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 아이템 조회 요청 - userId: {}, storeId: {}, receiptId: {}",
                userId, store.getId(), receiptId);

        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);
        ReceiptItem receiptItem = findReceiptItemByIdAndValidateAccess(receiptItemId, receipt);

        log.info("[ReceiptService] 입고 아이템 조회 성공 - receiptItemId: {}", receiptItem.getId());
        return ReceiptItemResponse.from(receiptItem);
    }

    @Transactional
    public ReceiptItemResponse updateReceiptItem(
            Long receiptId,
            Long receiptItemId,
            UpdateReceiptItemRequest request,
            Long userId
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 아이템 수정 요청 - userId: {}, storeId: {}, receiptId: {}",
                userId, store.getId(), receiptId);

        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);
        ReceiptItem receiptItem = findReceiptItemByIdAndValidateAccess(receiptItemId, receipt);

        receiptItem.update(request.actualQuantity(), request.note());

        log.info("[ReceiptService] 입고 아이템 수정 성공 - receiptItemId: {}", receiptItem.getId());
        return ReceiptItemResponse.from(receiptItem);
    }

    @Transactional
    public DeleteReceiptItemResponse deleteReceiptItem(Long receiptId, Long receiptItemId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ReceiptService] 입고 아이템 삭제 요청 - userId: {}, storeId: {}, receiptId: {}",
                userId, store.getId(), receiptId);

        Receipt receipt = findReceiptByIdAndValidateAccess(receiptId, store);
        ReceiptItem receiptItem = findReceiptItemByIdAndValidateAccess(receiptItemId, receipt);

        receipt.getItems().remove(receiptItem);
        inventoryService.cancelIncomingReservation(
                receiptItem.getProduct(), BigDecimal.valueOf(receiptItem.getExpectedQuantity())
        );

        log.info("[ReceiptService] 입고 아이템 삭제 성공 - receiptItemId: {}", receiptItemId);
        return new DeleteReceiptItemResponse(true);
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
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .deletedAt(null)
                .build();
    }

    private ReceiptItem toReceiptItemEntity(OrderItem orderItem) {
        return ReceiptItem.builder()
                .product(orderItem.getProduct())
                .expectedQuantity(orderItem.getQuantity())
                .actualQuantity(null)
                .unitPrice(orderItem.getUnitPrice())
                .amount(orderItem.getQuantity() * orderItem.getUnitPrice())
                .note(null)
                .build();
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

        if (receipt.getDeletedAt() != null) {
            throw new BaseException(ErrorCode.RECEIPT_NOT_FOUND);
        }
        if (!receipt.getStore().getId().equals(store.getId())) {
            throw new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED);
        }
        return receipt;
    }

    private Page<Receipt> findReceiptsByFilter(
            Long storeId, Long vendorId, ReceiptStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable
    ) {
        LocalDate startDate = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
        LocalDate endDate = toDate != null ? toDate : LocalDate.now();

        boolean hasVendor = vendorId != null;
        boolean hasStatus = status != null;

        // 1) 필터 없음
        if (!hasVendor && !hasStatus) {
            return receiptRepository.findAllByStoreIdAndReceiptDateBetween(
                    storeId, startDate, endDate, pageable
            );
        }

        // 2) 상태 필터
        if (!hasVendor) {
            return receiptRepository.findAllByStoreIdAndStatusAndReceiptDateBetween(
                    storeId, status, startDate, endDate, pageable
            );
        }

        // 3) 발주처 필터
        if (!hasStatus) {
            return receiptRepository.findAllByStoreIdAndOrderVendorIdAndReceiptDateBetween(
                    storeId, vendorId, startDate, endDate, pageable
            );
        }

        // 4) 발주처 + 상태 필터
        return receiptRepository.findAllByStoreIdAndOrderVendorIdAndStatusAndReceiptDateBetween(
                storeId, vendorId, status, startDate, endDate, pageable
        );
    }

    private void validateReceiptOrderNotChanged(Receipt receipt, Long requestOrderId) {
        if (requestOrderId == null) {
            return;
        }
        if (!receipt.getOrder().getId().equals(requestOrderId)) {
            throw new BaseException(ErrorCode.RECEIPT_ORDER_MISMATCH);
        }
    }

    private void updateReceiptItems(Receipt receipt, UpdateReceiptRequest request) {
        if (request.receiptItems() == null) {
            return;
        }

        for (UpdateReceiptItemRequest receiptItemRequest : request.receiptItems()) {
            ReceiptItem receiptItem =
                    findReceiptItemByIdAndValidateAccess(receiptItemRequest.receiptItemId(), receipt);
            receiptItem.update(
                    receiptItemRequest.actualQuantity(), receiptItemRequest.note()
            );
        }
    }

    private ReceiptItem findReceiptItemByIdAndValidateAccess(Long receiptItemId, Receipt receipt) {
        ReceiptItem receiptItem = receiptItemRepository.findById(receiptItemId)
                .orElseThrow(() -> new BaseException(ErrorCode.RECEIPT_ITEM_NOT_FOUND));

        if (!receiptItem.getReceipt().getId().equals(receipt.getId())) {
            throw new BaseException(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED);
        }
        return receiptItem;
    }
}
