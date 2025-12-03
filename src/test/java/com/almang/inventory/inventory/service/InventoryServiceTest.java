package com.almang.inventory.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.domain.InventoryMoveDirection;
import com.almang.inventory.inventory.domain.InventoryStatus;
import com.almang.inventory.inventory.dto.InitialInventoryValues;
import com.almang.inventory.inventory.dto.request.MoveInventoryRequest;
import com.almang.inventory.inventory.dto.request.UpdateInventoryRequest;
import com.almang.inventory.inventory.dto.response.InventoryResponse;
import com.almang.inventory.inventory.repository.InventoryRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class InventoryServiceTest {

    @Autowired private InventoryService inventoryService;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ProductRepository productRepository;

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
                        .phoneNumber("010-0000-0000")
                        .email(null)
                        .webPage(null)
                        .orderMethod("주문 방법")
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
                        .activated(true)
                        .costPrice(1000)
                        .retailPrice(1500)
                        .wholesalePrice(1200)
                        .build()
        );
    }

    @Test
    void 재고_수동_수정에_성공한다() {
        // given
        Store store = newStore("수동수정상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");
        Product product = newProduct(store, vendor, "상품1", "P001");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        BigDecimal newDisplay = BigDecimal.valueOf(30);
        BigDecimal newWarehouse = BigDecimal.valueOf(40);
        BigDecimal newOutgoing = BigDecimal.valueOf(5);
        BigDecimal newIncoming = BigDecimal.valueOf(15);
        BigDecimal newReorderTrigger = BigDecimal.valueOf(30);

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                product.getId(),
                newDisplay,
                newWarehouse,
                newOutgoing,
                newIncoming,
                newReorderTrigger
        );

        // when
        InventoryResponse response = inventoryService.updateInventory(inventory.getId(), request, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.inventoryId()).isEqualTo(inventory.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.productCode()).isEqualTo(product.getCode());
        assertThat(response.displayStock()).isEqualByComparingTo(newDisplay);
        assertThat(response.warehouseStock()).isEqualByComparingTo(newWarehouse);
        assertThat(response.outgoingReserved()).isEqualByComparingTo(newOutgoing);
        assertThat(response.incomingReserved()).isEqualByComparingTo(newIncoming);
        assertThat(response.reorderTriggerPoint()).isEqualByComparingTo(newReorderTrigger);
        assertThat(response.inventoryStatus()).isEqualTo(InventoryStatus.NORMAL);

        Inventory updated = inventoryRepository.findById(inventory.getId())
                .orElseThrow();
        assertThat(updated.getDisplayStock()).isEqualByComparingTo(newDisplay);
        assertThat(updated.getWarehouseStock()).isEqualByComparingTo(newWarehouse);
        assertThat(updated.getOutgoingReserved()).isEqualByComparingTo(newOutgoing);
        assertThat(updated.getIncomingReserved()).isEqualByComparingTo(newIncoming);
        assertThat(updated.getReorderTriggerPoint()).isEqualByComparingTo(newReorderTrigger);
    }

    @Test
    void 재고_수동_수정시_품목_아이디가_다르면_예외가_발생한다() {
        // given
        Store store = newStore("상품불일치상점");
        User user = newUser(store, "mismatchUser");
        Vendor vendor = newVendor(store, "발주처");

        Product product1 = newProduct(store, vendor, "상품1", "P001");
        Product product2 = newProduct(store, vendor, "상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product1, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product1.getId())
                .orElseThrow();

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                product2.getId(),
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );

        // when & then
        assertThatThrownBy(() -> inventoryService.updateInventory(inventory.getId(), request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_PRODUCT_MISMATCH.getMessage());
    }

    @Test
    void 재고_수동_수정시_다른_상점의_재고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");
        Product product2 = newProduct(store2, vendor2, "상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product2, initialInventoryValues);
        Inventory inventoryOfStore2 = inventoryRepository.findByProduct_Id(product2.getId())
                .orElseThrow();

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                product2.getId(),
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );

        // when & then
        assertThatThrownBy(() -> inventoryService.updateInventory(inventoryOfStore2.getId(), request, user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_ACCESS_DENIED.getMessage());
    }

    @Test
    void 재고_수동_수정시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        Long anyInventoryId = 1L;

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                null,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );

        // when & then
        assertThatThrownBy(() -> inventoryService.updateInventory(anyInventoryId, request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 재고_수동_수정시_재고가_존재하지_않으면_예외가_발생한다() {
        // given
        Store store = newStore("재고없음상점");
        User user = newUser(store, "noInventoryUser");
        Long notExistInventoryId = 9999L;

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                null,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );

        // when & then
        assertThatThrownBy(() -> inventoryService.updateInventory(notExistInventoryId, request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_NOT_FOUND.getMessage());
    }

    @Test
    void 재고_ID로_재고_조회에_성공한다() {
        // given
        Store store = newStore("조회상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");
        Product product = newProduct(store, vendor, "상품1", "P001");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        // when
        InventoryResponse response = inventoryService.getInventory(inventory.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.inventoryId()).isEqualTo(inventory.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.productCode()).isEqualTo(product.getCode());
        assertThat(response.displayStock()).isEqualByComparingTo(inventory.getDisplayStock());
        assertThat(response.warehouseStock()).isEqualByComparingTo(inventory.getWarehouseStock());
        assertThat(response.outgoingReserved()).isEqualByComparingTo(inventory.getOutgoingReserved());
        assertThat(response.incomingReserved()).isEqualByComparingTo(inventory.getIncomingReserved());
        assertThat(response.reorderTriggerPoint()).isEqualByComparingTo(inventory.getReorderTriggerPoint());

    }

    @Test
    void 재고_ID로_재고_조회시_다른_상점의_재고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");
        Product product2 = newProduct(store2, vendor2, "상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product2, initialInventoryValues);
        Inventory inventoryOfStore2 = inventoryRepository.findByProduct_Id(product2.getId())
                .orElseThrow();

        // when & then
        assertThatThrownBy(() -> inventoryService.getInventory(inventoryOfStore2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_ACCESS_DENIED.getMessage());
    }

    @Test
    void 품목으로_재고_조회에_성공한다() {
        // given
        Store store = newStore("품목조회상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");
        Product product = newProduct(store, vendor, "상품1", "P001");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        // when
        InventoryResponse response = inventoryService.getInventoryByProduct(product.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.inventoryId()).isEqualTo(inventory.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.productCode()).isEqualTo(product.getCode());
        assertThat(response.displayStock()).isEqualByComparingTo(inventory.getDisplayStock());
        assertThat(response.warehouseStock()).isEqualByComparingTo(inventory.getWarehouseStock());
        assertThat(response.outgoingReserved()).isEqualByComparingTo(inventory.getOutgoingReserved());
        assertThat(response.incomingReserved()).isEqualByComparingTo(inventory.getIncomingReserved());
        assertThat(response.reorderTriggerPoint()).isEqualByComparingTo(inventory.getReorderTriggerPoint());
    }

    @Test
    void 품목으로_재고_조회시_다른_상점의_재고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");
        Product product2 = newProduct(store2, vendor2, "상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product2, initialInventoryValues);

        // when & then
        assertThatThrownBy(() -> inventoryService.getInventoryByProduct(product2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_ACCESS_DENIED.getMessage());
    }

    @Test
    void 상점_재고_전체_조회에_성공한다() {
        // given
        Store store = newStore("재고목록상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");

        Product product1 = newProduct(store, vendor, "상품A", "P001");
        Product product2 = newProduct(store, vendor, "상품B", "P002");
        Product product3 = newProduct(store, vendor, "상품C", "P003");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product1, initialInventoryValues);
        inventoryService.createInventory(product2, initialInventoryValues);
        inventoryService.createInventory(product3, initialInventoryValues);

        Store otherStore = newStore("다른상점");
        Vendor otherVendor = newVendor(otherStore, "다른발주처");
        Product otherProduct = newProduct(otherStore, otherVendor, "다른상품", "PX01");
        inventoryService.createInventory(otherProduct, initialInventoryValues);

        // when
        PageResponse<InventoryResponse> pageResponse =
                inventoryService.getStoreInventoryList(user.getId(), 0, 20, "all", null, null);

        // then
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.content()).hasSize(3);

        assertThat(pageResponse.content())
                .extracting(InventoryResponse::productId)
                .containsExactlyInAnyOrder(
                        product1.getId(),
                        product2.getId(),
                        product3.getId()
                );

        assertThat(pageResponse.content())
                .extracting(InventoryResponse::productName)
                .containsExactlyInAnyOrder("상품A", "상품B", "상품C");
    }

    @Test
    void 상점_재고_목록_조회시_검색어로_필터링된다() {
        // given
        Store store = newStore("검색상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");

        Product target = newProduct(store, vendor, "고체치약 60ea", "P001");
        Product other1 = newProduct(store, vendor, "고무장갑 M", "P002");
        Product other2 = newProduct(store, vendor, "실리콘 용기 540ml", "P003");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(target, initialInventoryValues);
        inventoryService.createInventory(other1, initialInventoryValues);
        inventoryService.createInventory(other2, initialInventoryValues);

        // when
        PageResponse<InventoryResponse> pageResponse =
                inventoryService.getStoreInventoryList(user.getId(), 0, 20, "all", "치약", null);

        // then
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.content()).hasSize(1);

        InventoryResponse result = pageResponse.content().get(0);
        assertThat(result.productId()).isEqualTo(target.getId());
        assertThat(result.productName()).isEqualTo("고체치약 60ea");
    }

    @Test
    void 상점_재고_목록_조회시_scope가_적용된다() {
        // given
        Store store = newStore("스코프상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");

        Product displayProduct = newProduct(store, vendor, "매대상품", "P001");
        Product warehouseProduct = newProduct(store, vendor, "창고상품", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(displayProduct, initialInventoryValues);
        inventoryService.createInventory(warehouseProduct, initialInventoryValues);

        Inventory displayInventory = inventoryRepository.findByProduct_Id(displayProduct.getId())
                .orElseThrow();
        Inventory warehouseInventory = inventoryRepository.findByProduct_Id(warehouseProduct.getId())
                .orElseThrow();

        // 매대상품: display_stock > 0
        UpdateInventoryRequest displayUpdate = new UpdateInventoryRequest(
                displayProduct.getId(),
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        inventoryService.updateInventory(displayInventory.getId(), displayUpdate, user.getId());

        // 창고상품: warehouse_stock > 0, display_stock = 0
        UpdateInventoryRequest warehouseUpdate = new UpdateInventoryRequest(
                warehouseProduct.getId(),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        inventoryService.updateInventory(warehouseInventory.getId(), warehouseUpdate, user.getId());

        // when: scope = display
        PageResponse<InventoryResponse> pageResponse =
                inventoryService.getStoreInventoryList(user.getId(), 0, 20, "display", null, null);

        // then
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.content()).hasSize(1);

        InventoryResponse only = pageResponse.content().get(0);
        assertThat(only.productId()).isEqualTo(displayProduct.getId());
        assertThat(only.productName()).isEqualTo("매대상품");
    }

    @Test
    void 상점_재고_목록_조회시_상품명_기준_오름차순_정렬이_적용된다() {
        // given
        Store store = newStore("정렬상점");
        User user = newUser(store, "sortUser");
        Vendor vendor = newVendor(store, "발주처");

        Product p3 = newProduct(store, vendor, "치약", "P003");
        Product p1 = newProduct(store, vendor, "고무장갑", "P001");
        Product p2 = newProduct(store, vendor, "실리콘 용기", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(p1, initialInventoryValues);
        inventoryService.createInventory(p2, initialInventoryValues);
        inventoryService.createInventory(p3, initialInventoryValues);

        // when: sort=productName
        PageResponse<InventoryResponse> response =
                inventoryService.getStoreInventoryList(user.getId(), 0, 20, "all", null, "productName");

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(3);

        // 오름차순 정렬 결과 체크
        assertThat(response.content())
                .extracting(InventoryResponse::productName)
                .containsExactly("고무장갑", "실리콘 용기", "치약");
    }

    @Test
    void 창고에서_매대로_재고_이동에_성공한다() {
        // given
        Store store = newStore("이동상점");
        User user = newUser(store, "moveUser");
        Vendor vendor = newVendor(store, "발주처");
        Product product = newProduct(store, vendor, "이동상품", "P001");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest initRequest = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        inventoryService.updateInventory(inventory.getId(), initRequest, user.getId());

        // when
        MoveInventoryRequest moveRequest = new MoveInventoryRequest(
                BigDecimal.valueOf(3),
                InventoryMoveDirection.WAREHOUSE_TO_DISPLAY
        );
        InventoryResponse response = inventoryService.moveInventory(inventory.getId(), moveRequest, user.getId());

        // then
        assertThat(response.displayStock()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(response.warehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(7));

        Inventory updated = inventoryRepository.findById(inventory.getId())
                .orElseThrow();
        assertThat(updated.getDisplayStock()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(updated.getWarehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(7));
    }

    @Test
    void 매대에서_창고로_재고_이동에_성공한다() {
        // given
        Store store = newStore("이동상점2");
        User user = newUser(store, "moveUser2");
        Vendor vendor = newVendor(store, "발주처2");
        Product product = newProduct(store, vendor, "이동상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest initRequest = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(2),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        inventoryService.updateInventory(inventory.getId(), initRequest, user.getId());

        // when
        MoveInventoryRequest moveRequest = new MoveInventoryRequest(
                BigDecimal.valueOf(2),
                InventoryMoveDirection.DISPLAY_TO_WAREHOUSE
        );
        InventoryResponse response = inventoryService.moveInventory(inventory.getId(), moveRequest, user.getId());

        // then
        assertThat(response.displayStock()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(response.warehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(4));

        Inventory updated = inventoryRepository.findById(inventory.getId())
                .orElseThrow();
        assertThat(updated.getDisplayStock()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(updated.getWarehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(4));
    }

    @Test
    void 재고_이동시_다른_상점의_재고면_접근_거부_예외가_발생한다() {
        // given
        Store store1 = newStore("상점1");
        Store store2 = newStore("상점2");

        User user1 = newUser(store1, "user1");
        Vendor vendor2 = newVendor(store2, "발주처2");
        Product product2 = newProduct(store2, vendor2, "상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product2, initialInventoryValues);
        Inventory inventoryOfStore2 = inventoryRepository.findByProduct_Id(product2.getId())
                .orElseThrow();

        MoveInventoryRequest moveRequest = new MoveInventoryRequest(
                BigDecimal.ONE, InventoryMoveDirection.WAREHOUSE_TO_DISPLAY
        );

        // when & then
        assertThatThrownBy(() -> inventoryService.moveInventory(inventoryOfStore2.getId(), moveRequest, user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_ACCESS_DENIED.getMessage());
    }

    @Test
    void 창고에서_매대로_이동시_창고_재고보다_많이_요청하면_예외가_발생한다() {
        // given
        Store store = newStore("이동상점_예외");
        User user = newUser(store, "moveUser_ex");
        Vendor vendor = newVendor(store, "발주처");
        Product product = newProduct(store, vendor, "이동상품", "P001");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest initRequest = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.ZERO,
                BigDecimal.valueOf(5),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        inventoryService.updateInventory(inventory.getId(), initRequest, user.getId());

        // when
        MoveInventoryRequest moveRequest = new MoveInventoryRequest(
                BigDecimal.valueOf(10), InventoryMoveDirection.WAREHOUSE_TO_DISPLAY
        );

        // then
        assertThatThrownBy(() -> inventoryService.moveInventory(inventory.getId(), moveRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH.getMessage());

        Inventory after = inventoryRepository.findById(inventory.getId())
                .orElseThrow();
        assertThat(after.getDisplayStock()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(after.getWarehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void 매대에서_창고로_이동시_매대_재고보다_많이_요청하면_예외가_발생한다() {
        // given
        Store store = newStore("이동상점_예외2");
        User user = newUser(store, "moveUser_ex2");
        Vendor vendor = newVendor(store, "발주처2");
        Product product = newProduct(store, vendor, "이동상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);
        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest initRequest = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.valueOf(3),
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        inventoryService.updateInventory(inventory.getId(), initRequest, user.getId());

        // when
        MoveInventoryRequest moveRequest = new MoveInventoryRequest(
                BigDecimal.valueOf(5),
                InventoryMoveDirection.DISPLAY_TO_WAREHOUSE
        );

        // then
        assertThatThrownBy(() -> inventoryService.moveInventory(inventory.getId(), moveRequest, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH.getMessage());

        Inventory after = inventoryRepository.findById(inventory.getId())
                .orElseThrow();
        assertThat(after.getDisplayStock()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(after.getWarehouseStock()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void 재고_상태가_정상으로_계산된다() {
        // given
        Store store = newStore("상태상점1");
        User user = newUser(store, "statusUser1");
        Vendor vendor = newVendor(store, "발주처1");
        Product product = newProduct(store, vendor, "상품1", "P001");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest update = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(5),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3)
        );
        inventoryService.updateInventory(inventory.getId(), update, user.getId());

        // when
        InventoryResponse response =
                inventoryService.getInventory(inventory.getId(), user.getId());

        // then
        assertThat(response.inventoryStatus()).isEqualTo(InventoryStatus.NORMAL);
    }

    @Test
    void 재고_상태가_주문필요로_계산된다() {
        // given
        Store store = newStore("상태상점2");
        User user = newUser(store, "statusUser2");
        Vendor vendor = newVendor(store, "발주처2");
        Product product = newProduct(store, vendor, "상품2", "P002");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest update = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(3),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(5)
        );
        inventoryService.updateInventory(inventory.getId(), update, user.getId());

        // when
        InventoryResponse response =
                inventoryService.getInventory(inventory.getId(), user.getId());

        // then
        assertThat(response.inventoryStatus()).isEqualTo(InventoryStatus.LOW);
    }

    @Test
    void 재고_상태가_품절로_계산된다() {
        // given
        Store store = newStore("상태상점3");
        User user = newUser(store, "statusUser3");
        Vendor vendor = newVendor(store, "발주처3");
        Product product = newProduct(store, vendor, "상품3", "P003");

        InitialInventoryValues initialInventoryValues = new InitialInventoryValues(
                BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        inventoryService.createInventory(product, initialInventoryValues);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        UpdateInventoryRequest update = new UpdateInventoryRequest(
                product.getId(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(5)
        );
        inventoryService.updateInventory(inventory.getId(), update, user.getId());

        // when
        InventoryResponse response =
                inventoryService.getInventory(inventory.getId(), user.getId());

        // then
        assertThat(response.inventoryStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);
    }
}
