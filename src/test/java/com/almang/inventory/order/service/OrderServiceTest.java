package com.almang.inventory.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
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
    @Autowired private InventoryService inventoryService;
    @Autowired private InventoryRepository inventoryRepository;

    private Store newStore(String name) {
        return storeRepository.save(
                Store.builder()
                        .name(name)
                        .isActivate(true)
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
        Product product = productRepository.save(
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
        inventoryService.createInventory(product, BigDecimal.valueOf(30));

        return product;
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

        Inventory inventory1 = inventoryRepository.findByProduct_Id(product1.getId())
                .orElseThrow();
        Inventory inventory2 = inventoryRepository.findByProduct_Id(product2.getId())
                .orElseThrow();

        assertThat(inventory1.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(10)); // itemReq1 quantity
        assertThat(inventory2.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(5));  // itemReq2 quantity
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

    @Test
    void 발주_조회에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_reader");
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

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "카톡 발주 메시지입니다.",
                3,
                List.of(itemReq1, itemReq2)
        );

        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        // when
        OrderResponse response = orderService.getOrder(orderId, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(response.orderMessage()).isEqualTo("카톡 발주 메시지입니다.");
        assertThat(response.leadTime()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualTo(created.totalPrice());
        assertThat(response.activated()).isTrue();
        assertThat(response.orderItems()).hasSize(2);
    }

    @Test
    void 발주_조회시_발주가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_reader");
        Long notExistOrderId = 9999L;

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(notExistOrderId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_조회시_다른_상점의_발주라면_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        User userOfStore2 = newUser(store2, "user2");

        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");
        Product productOfStore2 = newProduct(store2, vendorOfStore2, "상점2 상품", "P999");

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
                productOfStore2.getId(),
                3,
                1000,
                "비고"
        );

        CreateOrderRequest request = new CreateOrderRequest(
                vendorOfStore2.getId(),
                "상점2의 발주",
                2,
                List.of(itemReq)
        );

        OrderResponse created = orderService.createOrder(request, userOfStore2.getId());
        Long orderId = created.orderId();

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(orderId, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_목록_조회_기본_조회에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_list_user");
        Vendor vendor = newVendor(store, "발주처1");

        Product p1 = newProduct(store, vendor, "상품1", "P001");
        Product p2 = newProduct(store, vendor, "상품2", "P002");

        CreateOrderRequest req1 = new CreateOrderRequest(
                vendor.getId(),
                "메시지1",
                1,
                List.of(new CreateOrderItemRequest(p1.getId(), 5, 1000, null))
        );
        orderService.createOrder(req1, user.getId());

        CreateOrderRequest req2 = new CreateOrderRequest(
                vendor.getId(),
                "메시지2",
                2,
                List.of(new CreateOrderItemRequest(p2.getId(), 3, 2000, null))
        );
        orderService.createOrder(req2, user.getId());

        // when
        PageResponse<OrderResponse> page = orderService.getOrderList(
                user.getId(),
                null,
                1,
                20,
                null,
                null,
                null
        );

        // then
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).hasSize(2);

        OrderResponse first = page.content().get(0);
        OrderResponse second = page.content().get(1);

        assertThat(first.orderMessage()).isEqualTo("메시지1");
        assertThat(second.orderMessage()).isEqualTo("메시지2");
    }

    @Test
    void 발주_목록_조회시_발주처로_필터링된다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_list_user");
        Vendor vendorA = newVendor(store, "A발주처");
        Vendor vendorB = newVendor(store, "B발주처");

        Product p1 = newProduct(store, vendorA, "상품1", "P001");
        Product p2 = newProduct(store, vendorB, "상품2", "P002");

        orderService.createOrder(
                new CreateOrderRequest(
                        vendorA.getId(), "A 요청", 1,
                        List.of(new CreateOrderItemRequest(p1.getId(), 5, 1000, null))
                ),
                user.getId()
        );
        orderService.createOrder(
                new CreateOrderRequest(
                        vendorB.getId(), "B 요청", 1,
                        List.of(new CreateOrderItemRequest(p2.getId(), 3, 2000, null))
                ),
                user.getId()
        );

        // when
        PageResponse<OrderResponse> page = orderService.getOrderList(
                user.getId(),
                vendorA.getId(),
                1,
                20,
                null,
                null,
                null
        );

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);

        OrderResponse first = page.content().get(0);
        assertThat(first.vendorId()).isEqualTo(vendorA.getId());
        assertThat(first.orderMessage()).isEqualTo("A 요청");
    }

    @Test
    void 발주_목록_조회시_상태로_필터링된다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_list_user");
        Vendor vendor = newVendor(store, "발주처1");

        Product product = newProduct(store, vendor, "상품1", "P001");

        orderService.createOrder(
                new CreateOrderRequest(
                        vendor.getId(), "REQUEST 메시지", 1,
                        List.of(new CreateOrderItemRequest(product.getId(), 1, 1000, null))
                ),
                user.getId()
        );
        Order saved = orderRepository.save(
                Order.builder()
                        .store(store)
                        .vendor(vendor)
                        .status(OrderStatus.IN_PRODUCTION)
                        .orderMessage("IN PRODUCTION 메시지")
                        .leadTime(1)
                        .expectedArrival(LocalDate.now().plusDays(1))
                        .activated(true)
                        .totalPrice(3000)
                        .build()
        );

        // when
        PageResponse<OrderResponse> page = orderService.getOrderList(
                user.getId(),
                null,
                1,
                20,
                OrderStatus.REQUEST,
                null,
                null
        );

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);

        OrderResponse first = page.content().get(0);
        assertThat(first.orderStatus()).isEqualTo(OrderStatus.REQUEST);
        assertThat(first.orderMessage()).isEqualTo("REQUEST 메시지");
    }

    @Test
    void 발주_목록_조회시_날짜_필터링_적용된다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_list_user");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품", "P001");

        orderService.createOrder(
                new CreateOrderRequest(
                        vendor.getId(), "오늘 발주", 1,
                        List.of(new CreateOrderItemRequest(product.getId(), 1, 1000, null))
                ),
                user.getId()
        );

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // when
        PageResponse<OrderResponse> page = orderService.getOrderList(
                user.getId(),
                null,
                1,
                20,
                null,
                yesterday,
                null
        );

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);

        OrderResponse first = page.content().get(0);
        assertThat(first.orderMessage()).isEqualTo("오늘 발주");
    }

    @Test
    void 발주_목록_조회시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        // when & then
        assertThatThrownBy(() -> orderService.getOrderList(
                notExistUserId, null, 1, 20, null, null, null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_수정에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "원본 메시지",
                3,
                List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, "원본 비고"))
        );
        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        Order savedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        OrderItem originalItem = savedOrder.getItems().get(0);
        Long orderItemId = originalItem.getId();

        int newQuantity = 5;
        int newUnitPrice = 2000;
        String newNote = "수정된 비고";
        int expectedTotalPrice = newQuantity * newUnitPrice;
        int newLeadTime = 5;
        LocalDate newQuoteDate = LocalDate.now();

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest(
                orderItemId,
                product.getId(),
                newQuantity,
                newUnitPrice,
                newNote
        );

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendor.getId(),
                OrderStatus.IN_PRODUCTION,
                "수정된 메시지",
                newLeadTime,
                newQuoteDate,
                null,
                false,
                List.of(updateItemRequest)
        );

        // when
        OrderResponse updated = orderService.updateOrder(orderId, updateRequest, user.getId());

        // then
        assertThat(updated.orderId()).isEqualTo(orderId);
        assertThat(updated.orderStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        assertThat(updated.orderMessage()).isEqualTo("수정된 메시지");
        assertThat(updated.leadTime()).isEqualTo(newLeadTime);
        assertThat(updated.expectedArrival()).isEqualTo(LocalDate.now().plusDays(newLeadTime));
        assertThat(updated.activated()).isFalse();
        assertThat(updated.totalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(updated.orderItems()).hasSize(1);

        OrderItem updatedItem = orderRepository.findById(orderId)
                .orElseThrow()
                .getItems()
                .get(0);

        assertThat(updatedItem.getQuantity()).isEqualTo(newQuantity);
        assertThat(updatedItem.getUnitPrice()).isEqualTo(newUnitPrice);
        assertThat(updatedItem.getAmount()).isEqualTo(newQuantity * newUnitPrice);
        assertThat(updatedItem.getNote()).isEqualTo(newNote);
    }

    @Test
    void 발주_수정시_발주가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Long notExistOrderId = 9999L;

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrder(notExistOrderId, updateRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_수정시_다른_상점의_발주라면_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        User userOfStore2 = newUser(store2, "user2");

        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");
        Product productOfStore2 = newProduct(store2, vendorOfStore2, "상점2 상품", "P999");

        // 상점2 유저가 발주 생성
        OrderResponse created = orderService.createOrder(
                new CreateOrderRequest(
                        vendorOfStore2.getId(),
                        "상점2 발주",
                        2,
                        List.of(new CreateOrderItemRequest(productOfStore2.getId(), 3, 1000, null))
                ),
                userOfStore2.getId()
        );

        Long orderId = created.orderId();

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendorOfStore2.getId(),
                OrderStatus.IN_PRODUCTION,
                "수정 시도",
                3,
                null,
                null,
                true,
                null
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrder(orderId, updateRequest, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_수정시_발주처를_변경하려_하면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendorA = newVendor(store, "A발주처");
        Vendor vendorB = newVendor(store, "B발주처");

        Product product = newProduct(store, vendorA, "상품1", "P001");

        OrderResponse created = orderService.createOrder(
                new CreateOrderRequest(
                        vendorA.getId(),
                        "원본 메시지",
                        2,
                        List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, null))
                ),
                user.getId()
        );

        Long orderId = created.orderId();

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendorB.getId(),
                OrderStatus.IN_PRODUCTION,
                "변경 시도",
                3,
                null,
                null,
                true,
                null
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrder(orderId, updateRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_CHANGE_NOT_ALLOWED.getMessage());
    }

    @Test
    void 발주_수정시_존재하지_않는_발주_항목이면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        OrderResponse created = orderService.createOrder(
                new CreateOrderRequest(
                        vendor.getId(),
                        "원본 메시지",
                        2,
                        List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, null))
                ),
                user.getId()
        );

        Long orderId = created.orderId();

        Long notExistOrderItemId = 9999L;
        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest(
                notExistOrderItemId,
                product.getId(),
                5,
                2000,
                "수정 비고"
        );

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendor.getId(),
                OrderStatus.IN_PRODUCTION,
                "수정 메시지",
                3,
                null,
                null,
                true,
                List.of(updateItemRequest)
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrder(orderId, updateRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_수정시_다른_발주의_항목이면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        OrderResponse order1 = orderService.createOrder(
                new CreateOrderRequest(
                        vendor.getId(),
                        "발주1",
                        2,
                        List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, null))
                ),
                user.getId()
        );

        OrderResponse order2 = orderService.createOrder(
                new CreateOrderRequest(
                        vendor.getId(),
                        "발주2",
                        2,
                        List.of(new CreateOrderItemRequest(product.getId(), 3, 1500, null))
                ),
                user.getId()
        );

        Order order2Entity = orderRepository.findById(order2.orderId())
                .orElseThrow();
        Long otherOrderItemId = order2Entity.getItems().get(0).getId();

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest(
                otherOrderItemId,
                product.getId(),
                10,
                2000,
                "수정 비고"
        );

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendor.getId(),
                OrderStatus.IN_PRODUCTION,
                "발주1 수정",
                3,
                null,
                null,
                true,
                List.of(updateItemRequest)
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrder(order1.orderId(), updateRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_상세_조회에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_item_reader");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "발주 메시지",
                3,
                List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, "비고"))
        );

        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow();
        OrderItem orderItem = order.getItems().get(0);
        Long orderItemId = orderItem.getId();

        // when
        OrderItemResponse response = orderService.getOrderItem(orderItemId, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderItemId()).isEqualTo(orderItemId);
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.unitPrice()).isEqualTo(1000);
        assertThat(response.amount()).isEqualTo(2 * 1000);
        assertThat(response.note()).isEqualTo("비고");
    }

    @Test
    void 발주_상세_조회시_발주_항목이_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_item_reader");
        Long notExistOrderItemId = 9999L;

        // when & then
        assertThatThrownBy(() -> orderService.getOrderItem(notExistOrderItemId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_상세_조회시_다른_상점의_발주_항목이면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        User userOfStore2 = newUser(store2, "user2");

        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");
        Product productOfStore2 = newProduct(store2, vendorOfStore2, "상점2 상품", "P999");

        OrderResponse created = orderService.createOrder(
                new CreateOrderRequest(
                        vendorOfStore2.getId(),
                        "상점2 발주",
                        2,
                        List.of(new CreateOrderItemRequest(productOfStore2.getId(), 3, 1000, "비고"))
                ),
                userOfStore2.getId()
        );

        Long orderId = created.orderId();
        Order order2 = orderRepository.findById(orderId)
                .orElseThrow();
        Long orderItemId = order2.getItems().get(0).getId();

        // when & then
        assertThatThrownBy(() -> orderService.getOrderItem(orderItemId, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_아이템_수정에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_item_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "발주 메시지",
                3,
                List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, "원본 비고"))
        );

        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        // 수정 전 입고 예정 재고 확인
        Inventory inventoryBefore = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();
        assertThat(inventoryBefore.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(2));

        Order order = orderRepository.findById(orderId)
                .orElseThrow();
        OrderItem orderItem = order.getItems().get(0);
        Long orderItemId = orderItem.getId();

        int newQuantity = 10;
        int newUnitPrice = 2000;
        String newNote = "수정된 비고";

        UpdateOrderItemRequest updateRequest = new UpdateOrderItemRequest(
                orderItemId,
                product.getId(),
                newQuantity,
                newUnitPrice,
                newNote
        );

        // when
        OrderItemResponse response = orderService.updateOrderItem(orderItemId, updateRequest, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderItemId()).isEqualTo(orderItemId);
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.quantity()).isEqualTo(newQuantity);
        assertThat(response.unitPrice()).isEqualTo(newUnitPrice);
        assertThat(response.amount()).isEqualTo(newQuantity * newUnitPrice);
        assertThat(response.note()).isEqualTo(newNote);

        OrderItem updated = orderRepository.findById(orderId)
                .orElseThrow()
                .getItems()
                .get(0);

        assertThat(updated.getQuantity()).isEqualTo(newQuantity);
        assertThat(updated.getUnitPrice()).isEqualTo(newUnitPrice);
        assertThat(updated.getAmount()).isEqualTo(newQuantity * newUnitPrice);
        assertThat(updated.getNote()).isEqualTo(newNote);

        // 수정 후 입고 예정 재고 확인
        Inventory inventoryAfter = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();
        assertThat(inventoryAfter.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void 발주_수정시_수량이_증가하면_입고_예정_재고도_증가한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "원본 메시지",
                3,
                List.of(new CreateOrderItemRequest(product.getId(), 2, 1000, "원본 비고"))
        );
        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        Inventory inventoryBefore = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();
        assertThat(inventoryBefore.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(2));

        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderItem orderItem = order.getItems().get(0);
        Long orderItemId = orderItem.getId();

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest(
                orderItemId,
                product.getId(),
                5,  // 변경 수량
                1000,
                "수정 비고"
        );

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendor.getId(),
                OrderStatus.IN_PRODUCTION,
                "수정된 메시지",
                null,
                null,
                null,
                null,
                List.of(updateItemRequest)
        );

        // when
        orderService.updateOrder(orderId, updateRequest, user.getId());

        // then
        Inventory inventoryAfter = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        assertThat(inventoryAfter.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void 발주_수정시_수량이_감소하면_입고_예정_재고도_감소한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "원본 메시지",
                3,
                List.of(new CreateOrderItemRequest(product.getId(), 5, 1000, "원본 비고"))
        );
        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        Inventory inventoryBefore = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();
        assertThat(inventoryBefore.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(5));

        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderItem orderItem = order.getItems().get(0);
        Long orderItemId = orderItem.getId();

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest(
                orderItemId,
                product.getId(),
                2,      // 변경 수량
                1000,
                "수정 비고"
        );

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendor.getId(),
                OrderStatus.IN_PRODUCTION,
                "수정된 메시지",
                null,
                null,
                null,
                null,
                List.of(updateItemRequest)
        );

        // when
        orderService.updateOrder(orderId, updateRequest, user.getId());

        // then
        Inventory inventoryAfter = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        assertThat(inventoryAfter.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @Test
    void 발주_수정시_수량이_변경되지_않으면_입고_예정_재고는_그대로_유지된다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_updater");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "원본 메시지",
                3,
                List.of(new CreateOrderItemRequest(product.getId(), 3, 1000, "원본 비고"))
        );
        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        Inventory inventoryBefore = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();
        assertThat(inventoryBefore.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(3));

        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderItem orderItem = order.getItems().get(0);
        Long orderItemId = orderItem.getId();

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest(
                orderItemId,
                product.getId(),
                3, // 수량 변경 없음
                2000,
                "단가만 수정"
        );

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                vendor.getId(),
                OrderStatus.IN_PRODUCTION,
                "메시지 수정",
                null,
                null,
                null,
                null,
                List.of(updateItemRequest)
        );

        // when
        orderService.updateOrder(orderId, updateRequest, user.getId());

        // then
        Inventory inventoryAfter = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        assertThat(inventoryAfter.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    @Test
    void 발주_아이템_수정시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyOrderItemId = 1L;

        UpdateOrderItemRequest updateRequest = new UpdateOrderItemRequest(
                anyOrderItemId,
                1L,
                1,
                1000,
                "비고"
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrderItem(anyOrderItemId, updateRequest, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_아이템_수정시_발주_항목이_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_item_updater");
        Long notExistOrderItemId = 9999L;

        UpdateOrderItemRequest updateRequest = new UpdateOrderItemRequest(
                notExistOrderItemId,
                null,
                5,
                2000,
                "수정 비고"
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrderItem(notExistOrderItemId, updateRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_아이템_수정시_다른_상점의_발주_항목이면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        User userOfStore2 = newUser(store2, "user2");

        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");
        Product productOfStore2 = newProduct(store2, vendorOfStore2, "상점2 상품", "P999");

        OrderResponse created = orderService.createOrder(
                new CreateOrderRequest(
                        vendorOfStore2.getId(),
                        "상점2 발주",
                        2,
                        List.of(new CreateOrderItemRequest(productOfStore2.getId(), 3, 1000, "비고"))
                ),
                userOfStore2.getId()
        );

        Long orderId = created.orderId();
        Order order2 = orderRepository.findById(orderId)
                .orElseThrow();
        Long orderItemId = order2.getItems().get(0).getId();

        UpdateOrderItemRequest updateRequest = new UpdateOrderItemRequest(
                orderItemId,
                productOfStore2.getId(),
                10,
                2000,
                "수정 비고"
        );

        // when & then
        assertThatThrownBy(() -> orderService.updateOrderItem(orderItemId, updateRequest, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ITEM_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_삭제에_성공한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_deleter");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest createRequest = new CreateOrderRequest(
                vendor.getId(),
                "삭제 대상 발주",
                2,
                List.of(new CreateOrderItemRequest(product.getId(), 3, 1000, "비고"))
        );

        OrderResponse created = orderService.createOrder(createRequest, user.getId());
        Long orderId = created.orderId();

        // 삭제 전에 입고 예정 재고가 3으로 증가했는지 확인
        Inventory inventoryBeforeDelete = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        assertThat(inventoryBeforeDelete.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.valueOf(3));

        // when
        DeleteOrderResponse response = orderService.deleteOrder(orderId, user.getId());

        // then
        assertThat(response).isNotNull();

        Order deletedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        assertThat(deletedOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(deletedOrder.isActivated()).isFalse();

        // 삭제 후 입고 예정 재고가 0으로 돌아갔는지 확인
        Inventory inventoryAfterDelete = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        assertThat(inventoryAfterDelete.getIncomingReserved())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 발주_삭제시_이미_취소된_발주는_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_tester");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        CreateOrderRequest request = new CreateOrderRequest(
                vendor.getId(),
                "취소된 발주",
                2,
                List.of(new CreateOrderItemRequest(product.getId(), 3, 1000, null))
        );

        OrderResponse created = orderService.createOrder(request, user.getId());
        Long orderId = created.orderId();

        // 발주 취소
        orderService.deleteOrder(orderId, user.getId());

        // when & then
        assertThatThrownBy(() -> orderService.deleteOrder(orderId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ALREADY_CANCELED.getMessage());
    }

    @Test
    void 발주_삭제시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyOrderId = 1L;

        // when & then
        assertThatThrownBy(() -> orderService.deleteOrder(anyOrderId, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_삭제시_발주가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("테스트 상점");
        User user = newUser(store, "order_deleter");
        Long notExistOrderId = 9999L;

        // when & then
        assertThatThrownBy(() -> orderService.deleteOrder(notExistOrderId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주_삭제시_다른_상점의_발주라면_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User userOfStore1 = newUser(store1, "user1");
        User userOfStore2 = newUser(store2, "user2");

        Vendor vendorOfStore2 = newVendor(store2, "상점2 발주처");
        Product productOfStore2 = newProduct(store2, vendorOfStore2, "상점2 상품", "P999");

        OrderResponse created = orderService.createOrder(
                new CreateOrderRequest(
                        vendorOfStore2.getId(),
                        "상점2 발주",
                        2,
                        List.of(new CreateOrderItemRequest(productOfStore2.getId(), 3, 1000, "비고"))
                ),
                userOfStore2.getId()
        );

        Long orderId = created.orderId();

        // when & then
        assertThatThrownBy(() -> orderService.deleteOrder(orderId, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }
}
