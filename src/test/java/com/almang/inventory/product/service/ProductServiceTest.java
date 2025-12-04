package com.almang.inventory.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.ProductUnit;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.request.UpdateProductRequest;
import com.almang.inventory.product.dto.response.DeleteProductResponse;
import com.almang.inventory.product.dto.response.ProductResponse;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ProductServiceTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private Store newStore() {
        return storeRepository.save(
                Store.builder()
                        .name("테스트 상점")
                        .isActivate(true)
                        .build()
        );
    }

    private Vendor newVendor(Store store) {
        return vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("테스트 발주처")
                        .channel(VendorChannel.KAKAO)
                        .phoneNumber("010-0000-0000")
                        .email(null)
                        .webPage(null)
                        .orderMethod("주문 방법")
                        .note("테스트 메모")
                        .activated(true)
                        .build()
        );
    }

    private User newUser(Store store) {
        return userRepository.save(
                User.builder()
                        .store(store)
                        .username("tester")
                        .password("password")
                        .name("테스트 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );
    }

    @Test
    void 품목_생성에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        CreateProductRequest request = new CreateProductRequest(
                vendor.getId(),
                "고체치약",
                "P-001",
                ProductUnit.G,
                1000,
                1500,
                1200,
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        // when
        ProductResponse response = productService.createProduct(request, user.getId());

        // then
        assertThat(response.name()).isEqualTo("고체치약");
        assertThat(response.code()).isEqualTo("P-001");
        assertThat(response.unit()).isEqualTo(ProductUnit.G);
        assertThat(response.costPrice()).isEqualTo(1000);
        assertThat(response.retailPrice()).isEqualTo(1500);
        assertThat(response.wholesalePrice()).isEqualTo(1200);
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(response.isActivated()).isTrue();

        List<Inventory> inventories = inventoryRepository.findAll();
        assertThat(inventories).hasSize(1);

        Inventory inventory = inventories.get(0);
        assertThat(inventory.getProduct().getId()).isEqualTo(response.productId());
        assertThat(inventory.getDisplayStock()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(inventory.getWarehouseStock()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(inventory.getIncomingReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(inventory.getOutgoingReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(inventory.getReorderTriggerPoint()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void 사용자_존재하지_않으면_품목_생성시_예외가_발생한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);

        Long notExistUserId = 9999L;

        CreateProductRequest request = new CreateProductRequest(
                vendor.getId(),
                "고체치약",
                "P-001",
                ProductUnit.G,
                1000,
                1500,
                1200,
                BigDecimal.valueOf(30),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 발주처가_존재하지_않으면_품목_생성시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        Long notExistVendorId = 9999L;

        CreateProductRequest request = new CreateProductRequest(
                notExistVendorId,
                "고체치약",
                "P-001",
                ProductUnit.G,
                1000,
                1500,
                1200,
                BigDecimal.valueOf(30),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점의_발주처로_품목_생성시_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = storeRepository.save(
                Store.builder()
                        .name("다른 상점")
                        .isActivate(true)
                        .build()
        );

        Vendor vendorOfStore2 = newVendor(store2);
        User userOfStore1 = newUser(store1);

        CreateProductRequest request = new CreateProductRequest(
                vendorOfStore2.getId(),  // 다른 상점의 발주처
                "고체치약",
                "P-001",
                ProductUnit.G,
                1000,
                1500,
                1200,
                BigDecimal.valueOf(30),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }

    @Test
    void 품목_수정에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor1 = newVendor(store);
        Vendor vendor2 = newVendor(store);
        User user = newUser(store);

        CreateProductRequest createRequest = new CreateProductRequest(
                vendor1.getId(),
                "고체치약",
                "P-001",
                ProductUnit.G,
                1000,
                1500,
                1200,
                BigDecimal.valueOf(30),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        ProductResponse created = productService.createProduct(createRequest, user.getId());

        // when
        UpdateProductRequest updateRequest = new UpdateProductRequest(
                vendor2.getId(),
                "수정된 고체치약",
                "P-999",
                ProductUnit.ML,
                false,
                2000,
                2500,
                2200
        );

        ProductResponse updated = productService.updateProduct(created.productId(), updateRequest, user.getId());

        // then
        assertThat(updated.name()).isEqualTo("수정된 고체치약");
        assertThat(updated.code()).isEqualTo("P-999");
        assertThat(updated.unit()).isEqualTo(ProductUnit.ML);
        assertThat(updated.isActivated()).isFalse();
        assertThat(updated.costPrice()).isEqualTo(2000);
        assertThat(updated.retailPrice()).isEqualTo(2500);
        assertThat(updated.wholesalePrice()).isEqualTo(2200);
        assertThat(updated.vendorId()).isEqualTo(vendor2.getId());
    }

    @Test
    void 존재하지_않는_품목을_수정하려고_하면_예외가_발생한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        Long notExistProductId = 9999L;

        UpdateProductRequest request = new UpdateProductRequest(
                vendor.getId(),
                "변경 이름",
                "CODE",
                ProductUnit.G,
                true,
                1000,
                2000,
                1500
        );

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(notExistProductId, request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void 존재하지_않는_발주처로_수정시_예외가_발생한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        ProductResponse created = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        Long notExistVendorId = 9999L;

        UpdateProductRequest request = new UpdateProductRequest(
                notExistVendorId,
                "변경됨",
                "NEW",
                ProductUnit.G,
                true,
                2000,
                3000,
                2500
        );

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(created.productId(), request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점의_발주처로_수정하면_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendor1 = newVendor(store1);
        Vendor vendorOfStore2 = newVendor(store2);
        User user = newUser(store1);

        ProductResponse created = productService.createProduct(
                new CreateProductRequest(
                        vendor1.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        UpdateProductRequest request = new UpdateProductRequest(
                vendorOfStore2.getId(),
                "변경됨",
                "NEW",
                ProductUnit.ML,
                true,
                2000,
                3000,
                2500
        );

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(created.productId(), request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }

    @Test
    void 다른_상점의_품목을_수정하려고_하면_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendorOfStore1 = newVendor(store1);
        Vendor vendorOfStore2 = newVendor(store2);

        User userOfStore1 = newUser(store1);
        User userOfStore2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("tester_store2")
                        .password("password")
                        .name("상점2 관리자")
                        .role(UserRole.ADMIN)
                        .build()
        );

        ProductResponse productOfStore2 = productService.createProduct(
                new CreateProductRequest(
                        vendorOfStore2.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                userOfStore2.getId()
        );

        // when & then
        UpdateProductRequest request = new UpdateProductRequest(
                vendorOfStore1.getId(),
                "변경됨",
                "NEW",
                ProductUnit.ML,
                true,
                2000,
                3000,
                2500
        );

        assertThatThrownBy(() ->
                productService.updateProduct(productOfStore2.productId(), request, userOfStore1.getId())
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_ACCESS_DENIED.getMessage());
    }

    @Test
    void 품목_상세_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        ProductResponse created = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // when
        ProductResponse detail = productService.getProductDetail(created.productId(), user.getId());

        // then
        assertThat(detail.productId()).isEqualTo(created.productId());
        assertThat(detail.name()).isEqualTo("고체치약");
        assertThat(detail.code()).isEqualTo("P-001");
        assertThat(detail.unit()).isEqualTo(ProductUnit.G);
        assertThat(detail.costPrice()).isEqualTo(1000);
        assertThat(detail.retailPrice()).isEqualTo(1500);
        assertThat(detail.wholesalePrice()).isEqualTo(1200);
        assertThat(detail.storeId()).isEqualTo(store.getId());
        assertThat(detail.vendorId()).isEqualTo(vendor.getId());
        assertThat(detail.isActivated()).isTrue();
    }

    @Test
    void 존재하지_않는_품목_상세_조회시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistProductId = 9999L;

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(notExistProductId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점의_품목을_상세_조회하면_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendorOfStore2 = newVendor(store2);

        User userOfStore1 = newUser(store1);
        User userOfStore2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("detail_tester_store2")
                        .password("password")
                        .name("상점2 관리자(상세조회)")
                        .role(UserRole.ADMIN)
                        .build()
        );

        ProductResponse productOfStore2 = productService.createProduct(
                new CreateProductRequest(
                        vendorOfStore2.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                userOfStore2.getId()
        );

        // when & then
        assertThatThrownBy(() ->
                productService.getProductDetail(productOfStore2.productId(), userOfStore1.getId())
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_ACCESS_DENIED.getMessage());
    }

    @Test
    void 품목_목록_전체_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );
        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        1000,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );
        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "세제",
                        "P-003",
                        ProductUnit.ML,
                        3000,
                        4000,
                        3500,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, null, null);

        // then
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).hasSize(3);
    }

    @Test
    void 품목_목록_활성_필터_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        // 활성 품목
        ProductResponse active = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 비활성으로 바꿀 품목
        ProductResponse willBeInactive = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 고무장갑 비활성 처리
        productService.updateProduct(
                willBeInactive.productId(),
                new UpdateProductRequest(
                        vendor.getId(),
                        willBeInactive.name(),
                        willBeInactive.code(),
                        willBeInactive.unit(),
                        false,
                        willBeInactive.costPrice(),
                        willBeInactive.retailPrice(),
                        willBeInactive.wholesalePrice()
                ),
                user.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, true, null);

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).productId()).isEqualTo(active.productId());
        assertThat(page.content().get(0).isActivated()).isTrue();
    }

    @Test
    void 품목_목록_이름검색으로_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );
        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );
        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "세제",
                        "P-003",
                        ProductUnit.ML,
                        3000,
                        4000,
                        3500,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, null, "고");

        // then
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content())
                .extracting(ProductResponse::name)
                .allMatch(name -> name.contains("고"));
    }

    @Test
    void 품목_목록_활성_및_이름검색_동시_필터링_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        ProductResponse activeMatch = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        ProductResponse inactiveMatch = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 고무장갑 비활성 처리
        productService.updateProduct(
                inactiveMatch.productId(),
                new UpdateProductRequest(
                        vendor.getId(),
                        inactiveMatch.name(),
                        inactiveMatch.code(),
                        inactiveMatch.unit(),
                        false,
                        inactiveMatch.costPrice(),
                        inactiveMatch.retailPrice(),
                        inactiveMatch.wholesalePrice()
                ),
                user.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, true, "고");

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        ProductResponse only = page.content().get(0);
        assertThat(only.productId()).isEqualTo(activeMatch.productId());
        assertThat(only.name()).isEqualTo("고체치약");
        assertThat(only.isActivated()).isTrue();
    }

    @Test
    void 품목_목록_조회시_다른_상점_품목은_포함되지_않는다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendor1 = newVendor(store1);
        Vendor vendor2 = newVendor(store2);

        User user1 = newUser(store1);
        User user2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("tester_store2")
                        .password("password")
                        .name("상점2 관리자")
                        .role(UserRole.ADMIN)
                        .build()
        );

        // store1의 품목 2개
        productService.createProduct(
                new CreateProductRequest(
                        vendor1.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user1.getId()
        );
        productService.createProduct(
                new CreateProductRequest(
                        vendor1.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user1.getId()
        );

        // store2의 품목 1개
        productService.createProduct(
                new CreateProductRequest(
                        vendor2.getId(),
                        "세제",
                        "P-003",
                        ProductUnit.ML,
                        3000,
                        4000,
                        3500,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user2.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user1.getId(), 1, 10, null, null);

        // then
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content())
                .extracting(ProductResponse::storeId)
                .containsOnly(store1.getId());
    }

    @Test
    void 존재하지_않는_사용자_품목_목록_조회시_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        // when & then
        assertThatThrownBy(() ->
                productService.getProductList(notExistUserId, 1, 10, null, null)
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 품목_목록_비활성_필터_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        // 활성 품목
        ProductResponse active = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 비활성로 바꿀 품목
        ProductResponse willBeInactive = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 고무장갑 비활성 처리
        productService.updateProduct(
                willBeInactive.productId(),
                new UpdateProductRequest(
                        vendor.getId(),
                        willBeInactive.name(),
                        willBeInactive.code(),
                        willBeInactive.unit(),
                        false,
                        willBeInactive.costPrice(),
                        willBeInactive.retailPrice(),
                        willBeInactive.wholesalePrice()
                ),
                user.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, false, null);

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        ProductResponse only = page.content().get(0);
        assertThat(only.productId()).isEqualTo(willBeInactive.productId());
        assertThat(only.isActivated()).isFalse();
    }

    @Test
    void 품목_목록_비활성_및_이름검색_동시_필터링_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        // 활성 + 이름 매칭
        productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 비활성 + 이름 매칭
        ProductResponse inactiveMatch = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 비활성 + 이름 미매칭
        ProductResponse inactiveNonMatch = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "세제",
                        "P-003",
                        ProductUnit.ML,
                        3000,
                        4000,
                        3500,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // 고무장갑, 세제 둘 다 비활성 처리
        productService.updateProduct(
                inactiveMatch.productId(),
                new UpdateProductRequest(
                        vendor.getId(),
                        inactiveMatch.name(),
                        inactiveMatch.code(),
                        inactiveMatch.unit(),
                        false,
                        inactiveMatch.costPrice(),
                        inactiveMatch.retailPrice(),
                        inactiveMatch.wholesalePrice()
                ),
                user.getId()
        );

        productService.updateProduct(
                inactiveNonMatch.productId(),
                new UpdateProductRequest(
                        vendor.getId(),
                        inactiveNonMatch.name(),
                        inactiveNonMatch.code(),
                        inactiveNonMatch.unit(),
                        false,
                        inactiveNonMatch.costPrice(),
                        inactiveNonMatch.retailPrice(),
                        inactiveNonMatch.wholesalePrice()
                ),
                user.getId()
        );

        // when
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, false, "고");

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        ProductResponse only = page.content().get(0);
        assertThat(only.productId()).isEqualTo(inactiveMatch.productId());
        assertThat(only.name()).isEqualTo("고무장갑");
        assertThat(only.isActivated()).isFalse();
    }

    @Test
    void 품목_삭제에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        ProductResponse created = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        Long productId = created.productId();

        // when
        DeleteProductResponse deleteResponse = productService.deleteProduct(productId, user.getId());

        // then
        assertThat(deleteResponse.success()).isTrue();

        assertThatThrownBy(() -> productService.getProductDetail(productId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, null, null);

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void 존재하지_않는_품목_삭제시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistProductId = 9999L;

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(notExistProductId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점의_품목을_삭제하려고_하면_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendor1 = newVendor(store1);
        Vendor vendor2 = newVendor(store2);

        User userOfStore1 = newUser(store1);
        User userOfStore2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("delete_tester_store2")
                        .password("password")
                        .name("상점2 관리자(삭제)")
                        .role(UserRole.ADMIN)
                        .build()
        );

        ProductResponse productOfStore2 = productService.createProduct(
                new CreateProductRequest(
                        vendor2.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                userOfStore2.getId()
        );

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(productOfStore2.productId(), userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_ACCESS_DENIED.getMessage());
    }

    @Test
    void 삭제된_품목은_목록_및_상세에서_조회되지_않는다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        ProductResponse product = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        productService.deleteProduct(product.productId(), user.getId());

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(product.productId(), user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        // when & then
        PageResponse<ProductResponse> page =
                productService.getProductList(user.getId(), 1, 10, null, null);

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void 발주처별_품목_목록_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor1 = newVendor(store);
        Vendor vendor2 = newVendor(store);
        User user = newUser(store);

        productService.createProduct(
                new CreateProductRequest(
                        vendor1.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );
        productService.createProduct(
                new CreateProductRequest(
                        vendor1.getId(),
                        "고무장갑",
                        "P-002",
                        ProductUnit.EA,
                        500,
                        800,
                        600,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        productService.createProduct(
                new CreateProductRequest(
                        vendor2.getId(),
                        "세제",
                        "P-003",
                        ProductUnit.ML,
                        3000,
                        4000,
                        3500,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // when
        List<ProductResponse> products =
                productService.getProductsByVendor(vendor1.getId(), user.getId());

        // then
        assertThat(products).hasSize(2);
        assertThat(products)
                .extracting(ProductResponse::vendorId)
                .containsOnly(vendor1.getId());
        assertThat(products)
                .extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("고체치약", "고무장갑");
    }

    @Test
    void 발주처별_품목_목록_조회시_존재하지_않는_발주처이면_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistVendorId = 9999L;

        // when & then
        assertThatThrownBy(() -> productService.getProductsByVendor(notExistVendorId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 발주처별_품목_목록_조회시_다른_상점_발주처이면_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendorOfStore2 = newVendor(store2);
        User userOfStore1 = newUser(store1);

        // when & then
        assertThatThrownBy(() -> productService.getProductsByVendor(vendorOfStore2.getId(), userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }

    @Test
    void 품목의_발주처_조회에_성공한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        User user = newUser(store);

        ProductResponse product = productService.createProduct(
                new CreateProductRequest(
                        vendor.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                user.getId()
        );

        // when
        VendorResponse vendorResponse =
                productService.getVendorByProduct(product.productId(), user.getId());

        // then
        assertThat(vendorResponse.vendorId()).isEqualTo(vendor.getId());
        assertThat(vendorResponse.name()).isEqualTo("테스트 발주처");
        assertThat(vendorResponse.channel()).isEqualTo(VendorChannel.KAKAO);
        assertThat(vendorResponse.storeId()).isEqualTo(store.getId());
        assertThat(vendorResponse.activated()).isTrue();
    }

    @Test
    void 존재하지_않는_품목의_발주처_조회시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistProductId = 9999L;

        // when & then
        assertThatThrownBy(() -> productService.getVendorByProduct(notExistProductId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점_품목의_발주처_조회시_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        Vendor vendorOfStore2 = newVendor(store2);

        User userOfStore1 = newUser(store1);
        User userOfStore2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("vendor_lookup_store2")
                        .password("password")
                        .name("상점2 관리자(발주처 조회)")
                        .role(UserRole.ADMIN)
                        .build()
        );

        ProductResponse productOfStore2 = productService.createProduct(
                new CreateProductRequest(
                        vendorOfStore2.getId(),
                        "고체치약",
                        "P-001",
                        ProductUnit.G,
                        1000,
                        1500,
                        1200,
                        BigDecimal.valueOf(30),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                userOfStore2.getId()
        );

        // when & then
        assertThatThrownBy(() -> productService.getVendorByProduct(productOfStore2.productId(), userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_ACCESS_DENIED.getMessage());
    }
}
