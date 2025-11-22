package com.almang.inventory.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderItem;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.dto.request.CreateOrderItemRequest;
import com.almang.inventory.order.dto.request.CreateOrderRequest;
import com.almang.inventory.order.dto.response.OrderResponse;
import com.almang.inventory.order.repository.OrderRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.domain.ProductUnit;
import com.almang.inventory.product.repository.ProductRepository;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired private OrderService orderService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;

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

    @Test
    void 발주_생성에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_tester");
        Vendor vendor = newVendor(store, "발주처1");

        Product product1 = newProduct(store, vendor, "상품1", "P001");
        Product product2 = newProduct(store, vendor, "상품2", "P002");

        CreateOrderItemRequest itemReq1 = new CreateOrderItemRequest(
                product1.getId(),
                10,
                1000,
                "비고1"
        );
        CreateOrderItemRequest itemReq2 = new CreateOrderItemRequest(
                product2.getId(),
                5,
                2000,
                "비고2"
        );

        CreateOrderRequest request = new CreateOrderRequest(
                vendor.getId(),
                "카톡 발주 메시지입니다.",
                3, // leadTime (일)
                List.of(itemReq1, itemReq2)
        );

        int expectedTotalPrice = 10 * 1000 + 5 * 2000;
        LocalDate expectedArrival = LocalDate.now().plusDays(3);

        // when
        OrderResponse response = orderService.createOrder(request, user.getId());

        // then
        assertThat(response).isNotNull();

        Order savedOrder = orderRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(savedOrder.getStore().getId()).isEqualTo(store.getId());
        assertThat(savedOrder.getVendor().getId()).isEqualTo(vendor.getId());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.REQUEST);
        assertThat(savedOrder.getOrderMessage()).isEqualTo("카톡 발주 메시지입니다.");
        assertThat(savedOrder.getLeadTime()).isEqualTo(3);
        assertThat(savedOrder.getExpectedArrival()).isEqualTo(expectedArrival);
        assertThat(savedOrder.isActivated()).isTrue();
        assertThat(savedOrder.getTotalPrice()).isEqualTo(expectedTotalPrice);

        assertThat(savedOrder.getItems()).hasSize(2);

        OrderItem savedItem1 = savedOrder.getItems().get(0);
        OrderItem savedItem2 = savedOrder.getItems().get(1);

        assertThat(savedItem1.getOrder().getId()).isEqualTo(savedOrder.getId());
        assertThat(savedItem1.getProduct().getId()).isIn(product1.getId(), product2.getId());
        assertThat(savedItem1.getAmount()).isEqualTo(savedItem1.getQuantity() * savedItem1.getUnitPrice());

        assertThat(savedItem2.getOrder().getId()).isEqualTo(savedOrder.getId());
        assertThat(savedItem2.getProduct().getId()).isIn(product1.getId(), product2.getId());
        assertThat(savedItem2.getAmount()).isEqualTo(savedItem2.getQuantity() * savedItem2.getUnitPrice());
    }

    @Test
    void 발주_생성시_주문_항목이_비어있으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_tester");
        Vendor vendor = newVendor(store, "발주처1");

        CreateOrderRequest emptyItemsRequest = new CreateOrderRequest(
                vendor.getId(),
                "메시지",
                2,
                List.of() // 빈 리스트
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(emptyItemsRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_EMPTY.getMessage());
    }

    @Test
    void 발주_생성시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                "메시지",
                2,
                List.of(
                        new CreateOrderItemRequest(1L, 1, 1000, "비고")
                )
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_생성시_존재하지_않는_발주처면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_tester");

        Long notExistVendorId = 9999L;

        CreateOrderRequest request = new CreateOrderRequest(
                notExistVendorId,
                "메시지",
                2,
                List.of(
                        new CreateOrderItemRequest(1L, 1, 1000, "비고")
                )
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_생성시_다른_상점의_발주처면_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
                1L,
                1,
                1000,
                "비고"
        );

        CreateOrderRequest request = new CreateOrderRequest(
                vendorOfStore2.getId(),
                "메시지",
                2,
                List.of(itemReq)
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_생성시_존재하지_않는_상품이_포함되면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_tester");
        Vendor vendor = newVendor(store, "발주처1");

        Long notExistProductId = 9999L;

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
                notExistProductId,
                5,
                1000,
                "비고"
        );

        CreateOrderRequest request = new CreateOrderRequest(
                vendor.getId(),
                "메시지",
                2,
                List.of(itemReq)
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_생성시_다른_상점의_상품이_포함되면_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        Vendor vendorOfStore1 = newVendor(store1, "상점1 발주처");
        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");

        Product productOfStore2 = newProduct(store2, vendorOfStore2, "상점2 상품", "P999");

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
                productOfStore2.getId(),
                3,
                1000,
                "비고"
        );

        CreateOrderRequest request = new CreateOrderRequest(
                vendorOfStore1.getId(),
                "메시지",
                2,
                List.of(itemReq)
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_ACCESS_DENIED.getMessage());
    }
}
