package com.almang.inventory.receipt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
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
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
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
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ProductRepository productRepository;

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
}
