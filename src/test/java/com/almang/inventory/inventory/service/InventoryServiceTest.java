package com.almang.inventory.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
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
    void 재고_수동_수정에_성공한다() {
        // given
        Store store = newStore("수동수정상점");
        User user = newUser(store, "inventoryUser");
        Vendor vendor = newVendor(store, "발주처");
        Product product = newProduct(store, vendor, "상품1", "P001");

        inventoryService.createInventory(product);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        BigDecimal newDisplay = BigDecimal.valueOf(1.234);
        BigDecimal newWarehouse = BigDecimal.valueOf(10.000);
        BigDecimal newOutgoing = BigDecimal.valueOf(0.500);
        BigDecimal newIncoming = BigDecimal.valueOf(3.000);
        BigDecimal newReorderTrigger = BigDecimal.valueOf(0.25);

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                product.getId(),
                newDisplay,
                newWarehouse,
                newOutgoing,
                newIncoming,
                newReorderTrigger
        );

        // when
        InventoryResponse response =
                inventoryService.updateInventory(inventory.getId(), request, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.inventoryId()).isEqualTo(inventory.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.displayStock()).isEqualByComparingTo(newDisplay);
        assertThat(response.warehouseStock()).isEqualByComparingTo(newWarehouse);
        assertThat(response.outgoingReserved()).isEqualByComparingTo(newOutgoing);
        assertThat(response.incomingReserved()).isEqualByComparingTo(newIncoming);
        assertThat(response.reorderTriggerPoint()).isEqualByComparingTo(newReorderTrigger);

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

        inventoryService.createInventory(product1);
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

        inventoryService.createInventory(product2);
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

        inventoryService.createInventory(product);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        // when
        InventoryResponse response = inventoryService.getInventory(inventory.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.inventoryId()).isEqualTo(inventory.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
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

        inventoryService.createInventory(product2);
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

        inventoryService.createInventory(product);

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow();

        // when
        InventoryResponse response = inventoryService.getInventoryByProduct(product.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.inventoryId()).isEqualTo(inventory.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
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

        inventoryService.createInventory(product2);

        // when & then
        assertThatThrownBy(() -> inventoryService.getInventoryByProduct(product2.getId(), user1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_ACCESS_DENIED.getMessage());
    }
}
