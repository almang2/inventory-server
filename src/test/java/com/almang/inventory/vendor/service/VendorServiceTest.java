package com.almang.inventory.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.response.VendorResponse;
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
class VendorServiceTest {

    @Autowired private VendorService vendorService;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;

    private Store newStore() {
        return storeRepository.save(
                Store.builder()
                        .name("테스트 상점")
                        .isActivate(true)
                        .defaultCountCheckThreshold(BigDecimal.valueOf(0.2))
                        .build()
        );
    }

    private User newUser(Store store) {
        return userRepository.save(
                User.builder()
                        .store(store)
                        .username("vendor_tester")
                        .password("password")
                        .name("발주처 테스트 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );
    }

    @Test
    void 발주처_생성에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        CreateVendorRequest request = new CreateVendorRequest(
                "테스트 발주처",
                VendorChannel.KAKAO,
                "010-0000-0000",
                "비고 메모"
        );

        // when
        VendorResponse response = vendorService.createVendor(request, user.getId());

        // then
        assertThat(response.name()).isEqualTo("테스트 발주처");
        assertThat(response.channel()).isEqualTo(VendorChannel.KAKAO);
        assertThat(response.contactPoint()).isEqualTo("010-0000-0000");
        assertThat(response.note()).isEqualTo("비고 메모");
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.activated()).isTrue();

        Vendor saved = vendorRepository.findById(response.vendorId())
                .orElseThrow();
        assertThat(saved.getStore().getId()).isEqualTo(store.getId());
        assertThat(saved.getName()).isEqualTo("테스트 발주처");
    }

    @Test
    void 사용자가_존재하지_않으면_발주처_생성시_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        CreateVendorRequest request = new CreateVendorRequest(
                "테스트 발주처",
                VendorChannel.KAKAO,
                "010-0000-0000",
                "비고 메모"
        );

        // when & then
        assertThatThrownBy(() -> vendorService.createVendor(request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 비고없이_발주처_생성해도_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        CreateVendorRequest request = new CreateVendorRequest(
                "비고없는 발주처",
                VendorChannel.EMAIL,
                "vendor@test.com",
                null
        );

        // when
        VendorResponse response = vendorService.createVendor(request, user.getId());

        // then
        assertThat(response.name()).isEqualTo("비고없는 발주처");
        assertThat(response.channel()).isEqualTo(VendorChannel.EMAIL);
        assertThat(response.contactPoint()).isEqualTo("vendor@test.com");
        assertThat(response.note()).isNull();
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.activated()).isTrue();
    }
}
