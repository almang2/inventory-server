package com.almang.inventory.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.template.repository.OrderTemplateRepository;
import com.almang.inventory.order.template.domain.OrderTemplate;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.dto.request.CreateOrderTemplateRequest;
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
    @Autowired private OrderTemplateRepository orderTemplateRepository;

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

    @Test
    void 발주처_상세_조회에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Vendor vendor = newVendor(store);

        // when
        VendorResponse response = vendorService.getVendorDetail(vendor.getId(), user.getId());

        // then
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(response.name()).isEqualTo("기존 발주처");
        assertThat(response.channel()).isEqualTo(VendorChannel.KAKAO);
        assertThat(response.contactPoint()).isEqualTo("010-1111-1111");
        assertThat(response.note()).isEqualTo("원본 메모");
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.activated()).isTrue();
    }

    @Test
    void 존재하지_않는_발주처_상세_조회시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistVendorId = 9999L;

        // when & then
        assertThatThrownBy(() -> vendorService.getVendorDetail(notExistVendorId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점_발주처_상세_조회시_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        User userOfStore1 = newUser(store1);
        User userOfStore2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("detail_tester_store2")
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

        // when & then
        assertThatThrownBy(() ->
                vendorService.getVendorDetail(vendorOfStore2.getId(), userOfStore1.getId())
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주처_목록_전체_조회에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        vendorRepository.save(newVendor(store));
        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("두번째 발주처")
                        .channel(VendorChannel.EMAIL)
                        .contactPoint("email@test.com")
                        .note("메모")
                        .activated(true)
                        .build()
        );

        // when
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(user.getId(), 1, 20, null, null);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void 활성된_발주처만_조회된다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("활성 발주처")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-1111")
                        .note("메모")
                        .activated(true)
                        .build()
        );

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("비활성 발주처")
                        .channel(VendorChannel.EMAIL)
                        .contactPoint("email@test.com")
                        .note("메모2")
                        .activated(false)
                        .build()
        );

        // when
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(user.getId(), 1, 20, true, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).name()).isEqualTo("활성 발주처");
        assertThat(response.content().get(0).activated()).isTrue();
    }

    @Test
    void 이름_검색_필터로_조회된다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("사과 공장")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-2222")
                        .note("메모")
                        .activated(true)
                        .build()
        );

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("바나나 공장")
                        .channel(VendorChannel.EMAIL)
                        .contactPoint("010-3333-4444")
                        .note("메모2")
                        .activated(true)
                        .build()
        );

        // when
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(user.getId(), 1, 20, null, "사과");

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).name()).isEqualTo("사과 공장");
    }

    @Test
    void 활성여부와_이름_검색이_둘다_적용되어_조회된다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("사과 공장")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-2222")
                        .note("메모")
                        .activated(true)
                        .build()
        );

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("사과 비활성 공장")
                        .channel(VendorChannel.EMAIL)
                        .contactPoint("010-3333-4444")
                        .note("메모2")
                        .activated(false)
                        .build()
        );

        // when
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(user.getId(), 1, 20, true, "사과");

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).name()).isEqualTo("사과 공장");
        assertThat(response.content().get(0).activated()).isTrue();
    }

    @Test
    void 비활성된_발주처만_조회된다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("활성 발주처")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-1111")
                        .note("메모")
                        .activated(true)
                        .build()
        );

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("비활성 발주처")
                        .channel(VendorChannel.EMAIL)
                        .contactPoint("email@test.com")
                        .note("메모2")
                        .activated(false)
                        .build()
        );

        // when
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(user.getId(), 1, 20, false, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).name()).isEqualTo("비활성 발주처");
        assertThat(response.content().get(0).activated()).isFalse();
    }

    @Test
    void 비활성여부와_이름_검색이_둘다_적용되어_조회된다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("사과 활성 공장")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-1111")
                        .note("메모1")
                        .activated(true)
                        .build()
        );

        vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("사과 비활성 공장")
                        .channel(VendorChannel.EMAIL)
                        .contactPoint("010-2222-2222")
                        .note("메모2")
                        .activated(false)
                        .build()
        );

        // when
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(user.getId(), 1, 20, false, "사과");

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).name()).isEqualTo("사과 비활성 공장");
        assertThat(response.content().get(0).activated()).isFalse();
    }

    @Test
    void 발주_템플릿_생성에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Vendor vendor = newVendor(store);

        CreateOrderTemplateRequest request = new CreateOrderTemplateRequest(
                "발주 제목",
                "발주 본문 내용입니다."
        );

        // when
        OrderTemplateResponse response =
                vendorService.createOrderTemplate(vendor.getId(), request, user.getId());

        // then
        assertThat(response.orderTemplateId()).isNotNull();
        assertThat(response.title()).isEqualTo("발주 제목");
        assertThat(response.body()).isEqualTo("발주 본문 내용입니다.");
        assertThat(response.activated()).isTrue();
        assertThat(response.vendorId()).isEqualTo(vendor.getId());

        OrderTemplate saved = orderTemplateRepository.findById(response.orderTemplateId())
                .orElseThrow();
        assertThat(saved.getVendor().getId()).isEqualTo(vendor.getId());
        assertThat(saved.getTitle()).isEqualTo("발주 제목");
        assertThat(saved.getBody()).isEqualTo("발주 본문 내용입니다.");
        assertThat(saved.isActivated()).isTrue();
    }

    @Test
    void 존재하지_않는_사용자로_발주_템플릿_생성시_예외가_발생한다() {
        // given
        Store store = newStore();
        Vendor vendor = newVendor(store);
        Long notExistUserId = 9999L;

        CreateOrderTemplateRequest request = new CreateOrderTemplateRequest(
                "발주 제목",
                "발주 본문"
        );

        // when & then
        assertThatThrownBy(() ->
                vendorService.createOrderTemplate(vendor.getId(), request, notExistUserId)
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 존재하지_않는_발주처로_발주_템플릿_생성시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistVendorId = 9999L;

        CreateOrderTemplateRequest request = new CreateOrderTemplateRequest(
                "발주 제목",
                "발주 본문"
        );

        // when & then
        assertThatThrownBy(() ->
                vendorService.createOrderTemplate(notExistVendorId, request, user.getId())
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점_발주처로_발주_템플릿_생성시_예외가_발생한다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        User userOfStore1 = newUser(store1);
        User userOfStore2 = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("template_tester2")
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

        CreateOrderTemplateRequest request = new CreateOrderTemplateRequest(
                "불법 템플릿",
                "해킹 시도"
        );

        // when & then
        assertThatThrownBy(() ->
                vendorService.createOrderTemplate(vendorOfStore2.getId(), request, userOfStore1.getId())
        )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.VENDOR_ACCESS_DENIED.getMessage());
    }
}
