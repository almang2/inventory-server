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
import com.almang.inventory.vendor.dto.request.UpdateVendorRequest;
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

    private Vendor newVendor(Store store) {
        return vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("기존 발주처")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-1111")
                        .note("원본 메모")
                        .activated(true)
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

    @Test
    void 발주처_수정에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Vendor vendor = newVendor(store);

        UpdateVendorRequest request = new UpdateVendorRequest(
                "수정된 발주처",
                VendorChannel.EMAIL,
                "vendor-updated@test.com",
                "수정된 메모",
                false
        );

        // when
        VendorResponse response = vendorService.updateVendor(vendor.getId(), request, user.getId());

        // then
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(response.name()).isEqualTo("수정된 발주처");
        assertThat(response.channel()).isEqualTo(VendorChannel.EMAIL);
        assertThat(response.contactPoint()).isEqualTo("vendor-updated@test.com");
        assertThat(response.note()).isEqualTo("수정된 메모");
        assertThat(response.activated()).isFalse();

        Vendor updated = vendorRepository.findById(vendor.getId())
                .orElseThrow();
        assertThat(updated.getName()).isEqualTo("수정된 발주처");
        assertThat(updated.getChannel()).isEqualTo(VendorChannel.EMAIL);
        assertThat(updated.getContactPoint()).isEqualTo("vendor-updated@test.com");
        assertThat(updated.getNote()).isEqualTo("수정된 메모");
        assertThat(updated.isActivated()).isFalse();
    }

    @Test
    void 존재하지_않는_발주처_수정시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistVendorId = 9999L;

        UpdateVendorRequest request = new UpdateVendorRequest(
                "수정_시도",
                VendorChannel.KAKAO,
                "010-9999-9999",
                "수정 메모",
                true
        );

        // when & then
        assertThatThrownBy(() -> vendorService.updateVendor(notExistVendorId, request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점_발주처_수정시_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

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

        Vendor vendorOfStore2 = vendorRepository.save(
                Vendor.builder()
                        .store(store2)
                        .name("상점2 발주처")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-2222-2222")
                        .note("상점2 메모")
                        .activated(true)
                        .build()
        );

        UpdateVendorRequest request = new UpdateVendorRequest(
                "불법 수정 시도",
                VendorChannel.EMAIL,
                "hacker@test.com",
                "해킹 시도",
                false
        );

        // when & then
        assertThatThrownBy(() -> vendorService.updateVendor(vendorOfStore2.getId(), request, userOfStore1.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }
}
