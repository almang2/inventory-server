package com.almang.inventory.receipt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.repository.OrderRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.domain.ProductUnit;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.receipt.domain.Receipt;
import com.almang.inventory.receipt.domain.ReceiptItem;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import com.almang.inventory.receipt.dto.request.UpdateReceiptItemRequest;
import com.almang.inventory.receipt.dto.request.UpdateReceiptRequest;
import com.almang.inventory.receipt.dto.response.ConfirmReceiptResponse;
import com.almang.inventory.receipt.dto.response.DeleteReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.ReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
import com.almang.inventory.receipt.repository.ReceiptItemRepository;
import com.almang.inventory.receipt.repository.ReceiptRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ReceiptServiceTest {

    @Autowired private ReceiptService receiptService;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private ReceiptItemRepository receiptItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private Store newStore(String name) {
        return storeRepository.save(
                Store.builder()
                        .name(name)
                        .isActivate(true)
                        .defaultCountCheckThreshold(BigDecimal.valueOf(0.2))
                        .build()
        );
    }

    private User newUser(Store store, String username) {
        return userRepository.save(
                User.builder()
                        .store(store)
                        .username(username)
                        .password("encoded-password")
                        .name("테스트 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );
    }

    private Vendor newVendor(Store store, String name) {
        return vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name(name)
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-1111")
                        .note("비고")
                        .activated(true)
                        .build()
        );
    }

    private Product newProduct(Store store, Vendor vendor, String name, String code) {
        return productRepository.save(
                Product.builder()
                        .store(store)
                        .vendor(vendor)
                        .name(name)
                        .code(code)
                        .unit(ProductUnit.EA)
                        .boxWeightG(null)
                        .unitPerBox(null)
                        .unitWeightG(null)
                        .activated(true)
                        .costPrice(1000)
                        .retailPrice(1500)
                        .wholesalePrice(1200)
                        .build()
        );
    }

    private Order newOrderWithItems(Store store, Vendor vendor) {
        Product product1 = newProduct(store, vendor, "상품1", "P001");
        Product product2 = newProduct(store, vendor, "상품2", "P002");

        Order order = Order.builder()
                .store(store)
                .vendor(vendor)
                .status(OrderStatus.REQUEST)
                .orderMessage("테스트 발주 메시지")
                .leadTime(2)
                .expectedArrival(null)
                .activated(true)
                .totalPrice(0)
                .build();

        OrderItem item1 = OrderItem.builder()
                .product(product1)
                .quantity(5)
                .unitPrice(1000)
                .amount(5000)
                .note("아이템1 비고")
                .build();

        OrderItem item2 = OrderItem.builder()
                .product(product2)
                .quantity(3)
                .unitPrice(2000)
                .amount(6000)
                .note("아이템2 비고")
                .build();

        order.addItem(item1);
        order.addItem(item2);

        return orderRepository.save(order);
    }

    private Inventory newInventory(Product product) {
        return inventoryRepository.save(
                Inventory.builder()
                        .product(product)
                        .displayStock(BigDecimal.ZERO)
                        .warehouseStock(BigDecimal.ZERO)
                        .outgoingReserved(BigDecimal.ZERO)
                        .incomingReserved(BigDecimal.ZERO)
                        .reorderTriggerPoint(null)
                        .build()
        );
    }

    @Test
    void 발주기반_입고_생성에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "receipt_tester");
        Vendor vendor = newVendor(store, "발주처1");

        Order order = newOrderWithItems(store, vendor);

        // when
        ReceiptResponse response = receiptService.createReceiptFromOrder(order.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.status()).isEqualTo(ReceiptStatus.PENDING);
        assertThat(response.activated()).isTrue();
        assertThat(response.receiptItems()).hasSize(2);

        Receipt saved = receiptRepository.findById(response.receiptId())
                .orElseThrow();

        assertThat(saved.getStore().getId()).isEqualTo(store.getId());
        assertThat(saved.getOrder().getId()).isEqualTo(order.getId());
        assertThat(saved.getItems()).hasSize(2);
    }

    @Test
    void 입고_생성시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyOrderId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.createReceiptFromOrder(anyOrderId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_생성시_발주가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "receipt_tester");
        Long notExistOrderId = 9999L;

        // when & then
        assertThatThrownBy(() -> receiptService.createReceiptFromOrder(notExistOrderId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_생성시_다른_상점의_발주면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");
        Order orderOfStore2 = newOrderWithItems(store2, vendor2);

        // when & then
        assertThatThrownBy(() -> receiptService.createReceiptFromOrder(orderOfStore2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }

    @Test
    void 취소된_발주에_대해_입고_생성시_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "receipt_tester");
        Vendor vendor = newVendor(store, "발주처1");

        Order order = newOrderWithItems(store, vendor);
        order.updateStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        // when & then
        assertThatThrownBy(() -> receiptService.createReceiptFromOrder(order.getId(), user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_CREATION_NOT_ALLOWED_FROM_ORDER.getMessage());
    }

    @Test
    void 발주기반_입고_조회에_성공한다() {
        // given
        Store store = newStore("상점A");
        User user = newUser(store, "testerA");
        Vendor vendor = newVendor(store, "발주처A");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : order.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt.addItem(item);
        }

        receiptRepository.save(receipt);

        // when
        ReceiptResponse response = receiptService.getReceiptFromOrder(order.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.receiptItems()).hasSize(2);
    }

    @Test
    void 발주기반_입고_조회시_입고가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("상점B");
        User user = newUser(store, "testerB");
        Vendor vendor = newVendor(store, "발주처B");

        Order order = newOrderWithItems(store, vendor);

        // when & then
        assertThatThrownBy(() -> receiptService.getReceiptFromOrder(order.getId(), user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_NOT_FOUND.getMessage());
    }

    @Test
    void 발주기반_입고_조회시_다른_상점의_발주면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");

        Order orderOfStore2 = newOrderWithItems(store2, vendor2);

        Receipt receipt = Receipt.builder()
                .store(store2)
                .order(orderOfStore2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receiptRepository.save(receipt);

        // when & then
        assertThatThrownBy(() -> receiptService.getReceiptFromOrder(orderOfStore2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주기반_입고_조회시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyOrderId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.getReceiptFromOrder(anyOrderId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_조회에_성공한다() {
        // given
        Store store = newStore("상점C");
        User user = newUser(store, "testerC");
        Vendor vendor = newVendor(store, "발주처C");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : order.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt.addItem(item);
        }

        Receipt saved = receiptRepository.save(receipt);

        // when
        ReceiptResponse response = receiptService.getReceipt(saved.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.receiptId()).isEqualTo(saved.getId());
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.status()).isEqualTo(ReceiptStatus.PENDING);
        assertThat(response.activated()).isTrue();
        assertThat(response.receiptItems()).hasSize(2);
    }

    @Test
    void 입고_조회시_입고가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("상점D");
        User user = newUser(store, "testerD");
        Long notExistReceiptId = 9999L;

        // when & then
        assertThatThrownBy(() -> receiptService.getReceipt(notExistReceiptId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_조회시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyReceiptId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.getReceipt(anyReceiptId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_조회시_다른_상점의_입고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");

        Order orderOfStore2 = newOrderWithItems(store2, vendor2);

        Receipt receiptOfStore2 = Receipt.builder()
                .store(store2)
                .order(orderOfStore2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : orderOfStore2.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receiptOfStore2.addItem(item);
        }

        receiptRepository.save(receiptOfStore2);

        // when & then
        assertThatThrownBy(() -> receiptService.getReceipt(receiptOfStore2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage());
    }

    @Test
    void 입고_목록_조회시_필터없이_해당_상점_입고만_반환한다() {
        // given
        Store store = newStore("상점_리스트");
        User user = newUser(store, "listUser");
        Vendor vendor1 = newVendor(store, "발주처1");
        Vendor vendor2 = newVendor(store, "발주처2");

        Order order1 = newOrderWithItems(store, vendor1);
        Receipt receipt1 = Receipt.builder()
                .store(store)
                .order(order1)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        for (OrderItem orderItem : order1.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt1.addItem(item);
        }
        receiptRepository.save(receipt1);

        Order order2 = newOrderWithItems(store, vendor2);
        Receipt receipt2 = Receipt.builder()
                .store(store)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(2)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        for (OrderItem orderItem : order2.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt2.addItem(item);
        }
        receiptRepository.save(receipt2);

        Store otherStore = newStore("다른상점");
        Vendor otherVendor = newVendor(otherStore, "다른발주처");
        Order otherOrder = newOrderWithItems(otherStore, otherVendor);
        Receipt otherReceipt = Receipt.builder()
                .store(otherStore)
                .order(otherOrder)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receiptRepository.save(otherReceipt);

        // when
        PageResponse<ReceiptResponse> response = receiptService.getReceiptList(
                user.getId(),
                0,
                10,
                null,
                null,
                null,
                null
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(2);
        assertThat(response.content())
                .extracting(ReceiptResponse::storeId)
                .containsOnly(store.getId());
        assertThat(response.content())
                .extracting(ReceiptResponse::receiptId)
                .containsExactlyInAnyOrder(receipt1.getId(), receipt2.getId());
    }

    @Test
    void 입고_목록_조회시_발주처_필터를_적용할_수_있다() {
        // given
        Store store = newStore("상점_발주처필터");
        User user = newUser(store, "vendorFilterUser");
        Vendor vendor1 = newVendor(store, "필터발주처");
        Vendor vendor2 = newVendor(store, "다른발주처");

        Order order1 = newOrderWithItems(store, vendor1);
        Receipt receipt1 = Receipt.builder()
                .store(store)
                .order(order1)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        for (OrderItem orderItem : order1.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt1.addItem(item);
        }
        receiptRepository.save(receipt1);

        Order order2 = newOrderWithItems(store, vendor2);
        Receipt receipt2 = Receipt.builder()
                .store(store)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        for (OrderItem orderItem : order2.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt2.addItem(item);
        }
        receiptRepository.save(receipt2);

        // when
        PageResponse<ReceiptResponse> response = receiptService.getReceiptList(
                user.getId(),
                0,
                10,
                vendor1.getId(),
                null,
                null,
                null
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        ReceiptResponse only = response.content().get(0);
        assertThat(only.receiptId()).isEqualTo(receipt1.getId());
        assertThat(only.orderId()).isEqualTo(order1.getId());
    }

    @Test
    void 입고_목록_조회시_기간_필터를_적용할_수_있다() {
        // given
        Store store = newStore("상점_기간필터");
        User user = newUser(store, "dateFilterUser");
        Vendor vendor = newVendor(store, "발주처");

        Order oldOrder = newOrderWithItems(store, vendor);
        Receipt oldReceipt = Receipt.builder()
                .store(store)
                .order(oldOrder)
                .receiptDate(LocalDate.now().minusDays(7))
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receiptRepository.save(oldReceipt);

        Order recentOrder = newOrderWithItems(store, vendor);
        Receipt recentReceipt = Receipt.builder()
                .store(store)
                .order(recentOrder)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receiptRepository.save(recentReceipt);

        LocalDate fromDate = LocalDate.now().minusDays(1);
        LocalDate toDate = LocalDate.now();

        // when
        PageResponse<ReceiptResponse> response = receiptService.getReceiptList(
                user.getId(),
                0,
                10,
                null,
                null,
                fromDate,
                toDate
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        ReceiptResponse only = response.content().get(0);
        assertThat(only.receiptId()).isEqualTo(recentReceipt.getId());
    }

    @Test
    void 입고_수정에_성공한다() {
        // given
        Store store = newStore("상점_수정");
        User user = newUser(store, "updateUser");
        Vendor vendor = newVendor(store, "발주처_수정");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : order.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt.addItem(item);
        }

        Receipt saved = receiptRepository.save(receipt);
        ReceiptItem item1 = saved.getItems().get(0);
        ReceiptItem item2 = saved.getItems().get(1);

        UpdateReceiptItemRequest updateItem1 = new UpdateReceiptItemRequest(
                item1.getId(),
                saved.getId(),
                2,
                null,
                null,
                10,
                1100,
                "수정 비고1"
        );

        UpdateReceiptItemRequest updateItem2 = new UpdateReceiptItemRequest(
                item2.getId(),
                saved.getId(),
                3,
                null,
                null,
                5,
                2100,
                "수정 비고2"
        );

        BigDecimal newTotalWeight = BigDecimal.valueOf(123.456);

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                order.getId(),
                null,
                newTotalWeight,
                ReceiptStatus.CONFIRMED,
                true,
                java.util.List.of(updateItem1, updateItem2)
        );

        // when
        ReceiptResponse response =
                receiptService.updateReceipt(saved.getId(), request, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.receiptId()).isEqualTo(saved.getId());
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.status()).isEqualTo(ReceiptStatus.CONFIRMED);
        assertThat(response.totalWeightG()).isEqualTo(newTotalWeight);
        assertThat(response.totalBoxCount()).isEqualTo(5);
        assertThat(response.receiptItems()).hasSize(2);

        Receipt updated = receiptRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(updated.getTotalBoxCount()).isEqualTo(5);
        assertThat(updated.getStatus()).isEqualTo(ReceiptStatus.CONFIRMED);
        assertThat(updated.getTotalWeightG()).isEqualTo(newTotalWeight);

        ReceiptItem updatedItem1 = updated.getItems().stream()
                .filter(i -> i.getId().equals(item1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(updatedItem1.getBoxCount()).isEqualTo(2);
        assertThat(updatedItem1.getActualQuantity()).isEqualTo(10);
        assertThat(updatedItem1.getUnitPrice()).isEqualTo(1100);
        assertThat(updatedItem1.getAmount()).isEqualTo(10 * 1100);
        assertThat(updatedItem1.getErrorRate()).isNotNull();
    }

    @Test
    void 입고_수정시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyReceiptId = 1L;

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                1L,
                null,
                null,
                null,
                null,
                java.util.List.of()
        );

        // when & then
        assertThatThrownBy(() -> receiptService.updateReceipt(anyReceiptId, request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_수정시_입고가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("상점_입고없음");
        User user = newUser(store, "noReceiptUser");

        Long notExistReceiptId = 9999L;

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                1L,
                null,
                null,
                null,
                null,
                java.util.List.of()
        );

        // when & then
        assertThatThrownBy(() -> receiptService.updateReceipt(notExistReceiptId, request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_수정시_요청_발주ID와_입고의_발주ID가_다르면_예외가_발생한다() {
        // given
        Store store = newStore("상점_발주불일치");
        User user = newUser(store, "mismatchUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order1 = newOrderWithItems(store, vendor);
        Order order2 = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order1)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receiptRepository.save(receipt);

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                order2.getId(),
                null,
                null,
                null,
                null,
                java.util.List.of()
        );

        // when & then
        assertThatThrownBy(() -> receiptService.updateReceipt(receipt.getId(), request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ORDER_MISMATCH.getMessage());
    }

    @Test
    void 입고_수정시_다른_입고의_아이템을_수정하려고_하면_예외가_발생한다() {
        // given
        Store store = newStore("상점_아이템접근");
        User user = newUser(store, "itemAccessUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order1 = newOrderWithItems(store, vendor);
        Receipt receipt1 = Receipt.builder()
                .store(store)
                .order(order1)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        for (OrderItem orderItem : order1.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt1.addItem(item);
        }
        receiptRepository.save(receipt1);

        Order order2 = newOrderWithItems(store, vendor);
        Receipt receipt2 = Receipt.builder()
                .store(store)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        for (OrderItem orderItem : order2.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt2.addItem(item);
        }
        Receipt savedReceipt2 = receiptRepository.save(receipt2);
        ReceiptItem otherReceiptItem = savedReceipt2.getItems().get(0);

        UpdateReceiptItemRequest wrongItemRequest = new UpdateReceiptItemRequest(
                otherReceiptItem.getId(),
                receipt1.getId(),
                1,
                null,
                null,
                3,
                1000,
                "잘못된 수정 요청"
        );

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                receipt1.getOrder().getId(),
                null,
                null,
                ReceiptStatus.PENDING,
                true,
                java.util.List.of(wrongItemRequest)
        );

        // when & then
        assertThatThrownBy(() -> receiptService.updateReceipt(receipt1.getId(), request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getMessage());
    }

    @Test
    void 입고_삭제에_성공한다() {
        // given
        Store store = newStore("삭제상점");
        User user = newUser(store, "deleteUser");
        Vendor vendor = newVendor(store, "삭제발주처");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        receiptRepository.save(receipt);

        // when
        receiptService.deleteReceipt(receipt.getId(), user.getId());

        // then
        Receipt deleted = receiptRepository.findById(receipt.getId()).orElseThrow();
        assertThat(deleted.isActivated()).isFalse();
        assertThat(deleted.getStatus()).isEqualTo(ReceiptStatus.CANCELED);
    }

    @Test
    void 입고_삭제시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyReceiptId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.deleteReceipt(anyReceiptId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_삭제시_입고가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("삭제_입고없음");
        User user = newUser(store, "deleteUser");

        Long notExistReceiptId = 9999L;

        // when & then
        assertThatThrownBy(() -> receiptService.deleteReceipt(notExistReceiptId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_삭제시_다른_상점의_입고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");

        Order order2 = newOrderWithItems(store2, vendor2);

        Receipt receiptOfStore2 = Receipt.builder()
                .store(store2)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        receiptRepository.save(receiptOfStore2);

        // when & then
        assertThatThrownBy(() -> receiptService.deleteReceipt(receiptOfStore2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage());
    }

    @Test
    void 입고_아이템_조회에_성공한다() {
        // given
        Store store = newStore("아이템조회상점");
        User user = newUser(store, "itemUser");
        Vendor vendor = newVendor(store, "아이템발주처");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : order.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt.addItem(item);
        }

        Receipt savedReceipt = receiptRepository.save(receipt);
        ReceiptItem targetItem = savedReceipt.getItems().get(0);

        // when
        ReceiptItemResponse response =
                receiptService.getReceiptItem(savedReceipt.getId(), targetItem.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.receiptItemId()).isEqualTo(targetItem.getId());
        assertThat(response.receiptId()).isEqualTo(savedReceipt.getId());
        assertThat(response.productId()).isEqualTo(targetItem.getProduct().getId());
        assertThat(response.amount()).isEqualTo(targetItem.getAmount());
    }

    @Test
    void 입고_아이템_조회시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyReceiptItemId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.getReceiptItem(1L, anyReceiptItemId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_아이템_조회시_아이템이_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("아이템없음상점");
        User user = newUser(store, "noItemUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order = newOrderWithItems(store, vendor);
        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        Receipt savedReceipt = receiptRepository.save(receipt);

        Long notExistReceiptItemId = 9999L;

        // when & then
        assertThatThrownBy(() -> receiptService.getReceiptItem(savedReceipt.getId(), notExistReceiptItemId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_아이템_조회시_다른_상점의_아이템이면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");

        Order order2 = newOrderWithItems(store2, vendor2);

        Receipt receiptOfStore2 = Receipt.builder()
                .store(store2)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : order2.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receiptOfStore2.addItem(item);
        }

        Receipt savedReceipt2 = receiptRepository.save(receiptOfStore2);
        ReceiptItem itemOfStore2 = savedReceipt2.getItems().get(0);

        // when & then
        assertThatThrownBy(() -> receiptService.getReceiptItem(savedReceipt2.getId(), itemOfStore2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage());
    }

    @Test
    void 입고_아이템_수정에_성공한다() {
        // given
        Store store = newStore("아이템수정상점");
        User user = newUser(store, "modifyItemUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        for (OrderItem orderItem : order.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .product(orderItem.getProduct())
                    .expectedQuantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .amount(orderItem.getAmount())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            receipt.addItem(item);
        }

        Receipt saved = receiptRepository.save(receipt);
        ReceiptItem targetItem = saved.getItems().get(0);

        UpdateReceiptItemRequest req = new UpdateReceiptItemRequest(
                targetItem.getId(),
                saved.getId(),
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정된 비고"
        );

        // when
        ReceiptItemResponse response = receiptService.updateReceiptItem(
                saved.getId(), targetItem.getId(), req, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.boxCount()).isEqualTo(2);
        assertThat(response.actualQuantity()).isEqualTo(10);
        assertThat(response.unitPrice()).isEqualTo(1500);
        assertThat(response.amount()).isEqualTo(10 * 1500);

        ReceiptItem updated = receiptItemRepository.findById(targetItem.getId()).orElseThrow();
        assertThat(updated.getBoxCount()).isEqualTo(2);
        assertThat(updated.getActualQuantity()).isEqualTo(10);
        assertThat(updated.getUnitPrice()).isEqualTo(1500);
        assertThat(updated.getErrorRate()).isNotNull();
    }

    @Test
    void 입고_아이템_수정시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyItemId = 1L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                anyItemId, 1L, 1, null, null, 5, 1000, "비고"
        );

        // when & then
        assertThatThrownBy(() -> receiptService.updateReceiptItem(1L, anyItemId, request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_아이템_수정시_아이템이_해당_입고에_속하지_않으면_접근_거부_예외가_발생한다() {
        // given
        Store store = newStore("아이템불일치상점");
        User user = newUser(store, "wrongItemUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order1 = newOrderWithItems(store, vendor);
        Order order2 = newOrderWithItems(store, vendor);

        Receipt receipt1 = Receipt.builder()
                .store(store)
                .order(order1)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        receipt1.addItem(
                ReceiptItem.builder()
                        .product(order1.getItems().get(0).getProduct())
                        .expectedQuantity(BigDecimal.valueOf(5))
                        .amount(5000)
                        .unitPrice(1000)
                        .build()
        );
        Receipt savedReceipt1 = receiptRepository.save(receipt1);

        Receipt receipt2 = Receipt.builder()
                .store(store)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receipt2.addItem(
                ReceiptItem.builder()
                        .product(order2.getItems().get(0).getProduct())
                        .expectedQuantity(BigDecimal.valueOf(3))
                        .amount(6000)
                        .unitPrice(2000)
                        .build()
        );
        Receipt savedReceipt2 = receiptRepository.save(receipt2);
        ReceiptItem otherReceiptItem = savedReceipt2.getItems().get(0);

        UpdateReceiptItemRequest wrongRequest = new UpdateReceiptItemRequest(
                otherReceiptItem.getId(),
                savedReceipt1.getId(),
                1,
                null,
                null,
                10,
                2000,
                "잘못 수정"
        );

        // when & then
        assertThatThrownBy(() ->
                receiptService.updateReceiptItem(savedReceipt1.getId(), otherReceiptItem.getId(), wrongRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getMessage());
    }

    @Test
    void 입고_아이템_수정시_입고ID가_다르면_예외가_발생한다() {
        // given
        Store store = newStore("입고Id불일치상점");
        User user = newUser(store, "receiptMismatchUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receipt.addItem(
                ReceiptItem.builder()
                        .product(order.getItems().get(0).getProduct())
                        .expectedQuantity(BigDecimal.valueOf(5))
                        .amount(5000)
                        .unitPrice(1000)
                        .build()
        );

        Receipt saved = receiptRepository.save(receipt);
        ReceiptItem targetItem = saved.getItems().get(0);

        Long wrongReceiptId = saved.getId() + 999;

        UpdateReceiptItemRequest wrongReq = new UpdateReceiptItemRequest(
                targetItem.getId(),
                wrongReceiptId,
                1,
                null,
                null,
                5,
                1000,
                "비고"
        );

        // when & then
        assertThatThrownBy(() ->
                receiptService.updateReceiptItem(wrongReceiptId, targetItem.getId(), wrongReq, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_아이템_삭제에_성공한다() {
        // given
        Store store = newStore("아이템삭제상점");
        User user = newUser(store, "deleteItemUser");
        Vendor vendor = newVendor(store, "삭제발주처");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        ReceiptItem item1 = ReceiptItem.builder()
                .product(order.getItems().get(0).getProduct())
                .boxCount(2)
                .expectedQuantity(BigDecimal.valueOf(order.getItems().get(0).getQuantity()))
                .amount(order.getItems().get(0).getAmount())
                .unitPrice(order.getItems().get(0).getUnitPrice())
                .build();

        ReceiptItem item2 = ReceiptItem.builder()
                .product(order.getItems().get(1).getProduct())
                .boxCount(3)
                .expectedQuantity(BigDecimal.valueOf(order.getItems().get(1).getQuantity()))
                .amount(order.getItems().get(1).getAmount())
                .unitPrice(order.getItems().get(1).getUnitPrice())
                .build();

        receipt.addItem(item1);
        receipt.addItem(item2);
        receipt.updateTotalBoxCount(2 + 3);

        Receipt saved = receiptRepository.save(receipt);
        Long targetItemId = saved.getItems().get(0).getId();

        // when
        DeleteReceiptItemResponse response = receiptService.deleteReceiptItem(saved.getId(), targetItemId, user.getId());

        // then
        Receipt updated = receiptRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(updated.getItems())
                .extracting(ReceiptItem::getId)
                .doesNotContain(targetItemId);
        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getTotalBoxCount()).isEqualTo(3);
        assertThat(response.success()).isTrue();
    }

    @Test
    void 입고_아이템_삭제시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyReceiptItemId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.deleteReceiptItem(1L, anyReceiptItemId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_아이템_삭제시_아이템이_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("아이템삭제_아이템없음상점");
        User user = newUser(store, "noItemDeleteUser");
        Vendor vendor = newVendor(store, "발주처");

        Order order = newOrderWithItems(store, vendor);
        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(0)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        Receipt savedReceipt = receiptRepository.save(receipt);

        Long notExistReceiptItemId = 9999L;

        // when & then
        assertThatThrownBy(() -> receiptService.deleteReceiptItem(savedReceipt.getId(), notExistReceiptItemId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_아이템_삭제시_다른_상점의_아이템이면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");

        Order order2 = newOrderWithItems(store2, vendor2);

        Receipt receiptOfStore2 = Receipt.builder()
                .store(store2)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        ReceiptItem itemOfStore2 = ReceiptItem.builder()
                .product(order2.getItems().get(0).getProduct())
                .expectedQuantity(BigDecimal.valueOf(order2.getItems().get(0).getQuantity()))
                .amount(order2.getItems().get(0).getAmount())
                .unitPrice(order2.getItems().get(0).getUnitPrice())
                .build();
        receiptOfStore2.addItem(itemOfStore2);

        Receipt savedReceipt2 = receiptRepository.save(receiptOfStore2);
        Long targetItemId = savedReceipt2.getItems().get(0).getId();

        // when & then
        assertThatThrownBy(() -> receiptService.deleteReceiptItem(savedReceipt2.getId(), targetItemId, user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage());
    }

    @Test
    void 입고_확정에_성공한다() {
        // given
        Store store = newStore("확정상점");
        User user = newUser(store, "confirmUser");
        Vendor vendor = newVendor(store, "확정부발주처");

        Order order = newOrderWithItems(store, vendor);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        Receipt saved = receiptRepository.save(receipt);

        // when
        ConfirmReceiptResponse response = receiptService.confirmReceipt(saved.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        Receipt updated = receiptRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(ReceiptStatus.CONFIRMED);
        assertThat(updated.isActivated()).isTrue();
    }

    @Test
    void 입고_확정시_재고가_정상적으로_반영된다() {
        // given
        Store store = newStore("재고확인상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "재고발주처");

        Product product = newProduct(store, vendor, "상품1", "P001");

        Inventory inventory = newInventory(product);
        inventory.increaseIncoming(BigDecimal.valueOf(5));

        Order order = Order.builder()
                .store(store)
                .vendor(vendor)
                .status(OrderStatus.REQUEST)
                .orderMessage("테스트 발주")
                .activated(true)
                .totalPrice(0)
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(5)
                .unitPrice(1000)
                .amount(5000)
                .build();

        order.addItem(item);
        orderRepository.save(order);

        Receipt receipt = Receipt.builder()
                .store(store)
                .order(order)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();
        receiptRepository.save(receipt);

        // when
        receiptService.confirmReceipt(receipt.getId(), user.getId());

        // then
        Inventory updated = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();
        assertThat(updated.getIncomingReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updated.getWarehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void 입고_확정시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyReceiptId = 1L;

        // when & then
        assertThatThrownBy(() -> receiptService.confirmReceipt(anyReceiptId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_확정시_입고가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("확정_입고없음상점");
        User user = newUser(store, "noReceiptConfirmUser");
        Long notExistReceiptId = 9999L;

        // when & then
        assertThatThrownBy(() -> receiptService.confirmReceipt(notExistReceiptId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_NOT_FOUND.getMessage());
    }

    @Test
    void 입고_확정시_다른_상점의_입고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");

        Order order2 = newOrderWithItems(store2, vendor2);

        Receipt receiptOfStore2 = Receipt.builder()
                .store(store2)
                .order(order2)
                .receiptDate(LocalDate.now())
                .totalBoxCount(1)
                .totalWeightG(null)
                .status(ReceiptStatus.PENDING)
                .activated(true)
                .build();

        Receipt savedReceipt2 = receiptRepository.save(receiptOfStore2);

        // when & then
        assertThatThrownBy(() -> receiptService.confirmReceipt(savedReceipt2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage());
    }
}
