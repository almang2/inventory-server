package com.almang.inventory.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.product.domain.ProductUnit;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.response.ProductResponse;
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
public class ProductServiceTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private VendorRepository vendorRepository;

    private Store newStore() {
        return storeRepository.save(
                Store.builder()
                        .name("테스트 상점")
                        .isActivate(true)
                        .defaultCountCheckThreshold(BigDecimal.valueOf(0.2))
                        .build()
        );
    }

    private Vendor newVendor(Store store) {
        return vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("테스트 발주처")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-0000-0000")
                        .note("테스트 메모")
                        .isActivate(true)
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
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
        );

        // when
        ProductResponse response = productService.createProduct(request, user.getId());

        // then
        assertThat(response.name()).isEqualTo("고체치약");
        assertThat(response.code()).isEqualTo("P-001");
        assertThat(response.unit()).isEqualTo(ProductUnit.G);
        assertThat(response.boxWeightG()).isEqualByComparingTo("1000.0");
        assertThat(response.unitPerBox()).isEqualTo(10);
        assertThat(response.unitWeightG()).isEqualByComparingTo("100.0");
        assertThat(response.costPrice()).isEqualTo(1000);
        assertThat(response.retailPrice()).isEqualTo(1500);
        assertThat(response.wholesalePrice()).isEqualTo(1200);
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(response.isActivate()).isTrue();
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
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
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
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
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
                        .defaultCountCheckThreshold(BigDecimal.valueOf(0.2))
                        .build()
        );

        Vendor vendorOfStore2 = newVendor(store2);
        User userOfStore1 = newUser(store1);

        CreateProductRequest request = new CreateProductRequest(
                vendorOfStore2.getId(),  // 다른 상점의 발주처!
                "고체치약",
                "P-001",
                ProductUnit.G,
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }
}
