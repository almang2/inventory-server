package com.almang.inventory.receipt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
