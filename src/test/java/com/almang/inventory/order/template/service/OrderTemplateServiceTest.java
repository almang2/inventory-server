package com.almang.inventory.order.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.template.domain.OrderTemplate;
import com.almang.inventory.order.template.dto.request.UpdateOrderTemplateRequest;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.order.template.repository.OrderTemplateRepository;
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
class OrderTemplateServiceTest {

    @Autowired private OrderTemplateService orderTemplateService;
    @Autowired private OrderTemplateRepository orderTemplateRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private StoreRepository storeRepository;

    private Store newStore() {
        return storeRepository.save(
                Store.builder()
                        .name("테스트 상점")
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
                        .name("발주 템플릿 테스트 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );
    }

    private Vendor newVendor(Store store) {
        return vendorRepository.save(
                Vendor.builder()
                        .store(store)
                        .name("테스트 발주처")
                        .channel(VendorChannel.KAKAO)
                        .contactPoint("010-1111-1111")
                        .note("메모")
                        .activated(true)
                        .build()
        );
    }

    private OrderTemplate newOrderTemplate(Vendor vendor) {
        return orderTemplateRepository.save(
                OrderTemplate.builder()
                        .vendor(vendor)
                        .title("기존 제목")
                        .body("기존 본문")
                        .activated(true)
                        .build()
        );
    }

    @Test
    void 발주_템플릿_수정에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Vendor vendor = newVendor(store);
        OrderTemplate template = newOrderTemplate(vendor);

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                "수정된 제목",
                "수정된 본문",
                false
        );

        // when
        OrderTemplateResponse response =
                orderTemplateService.updateOrderTemplate(template.getId(), request, user.getId());

        // then
        assertThat(response.orderTemplateId()).isEqualTo(template.getId());
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.body()).isEqualTo("수정된 본문");
        assertThat(response.activated()).isFalse();

        OrderTemplate updated =
                orderTemplateRepository.findById(template.getId()).orElseThrow();

        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getBody()).isEqualTo("수정된 본문");
        assertThat(updated.isActivated()).isFalse();
    }

    @Test
    void 사용자가_없으면_발주_템플릿_수정시_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                "제목",
                "본문",
                true
        );

        // when & then
        assertThatThrownBy(() ->
                orderTemplateService.updateOrderTemplate(1L, request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 템플릿이_존재하지_않으면_발주_템플릿_수정시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                "제목",
                "본문",
                true
        );

        // when & then
        assertThatThrownBy(() ->
                orderTemplateService.updateOrderTemplate(9999L, request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_TEMPLATE_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점의_발주_템플릿이면_수정시_접근이_거부된다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        User store1User = newUser(store1);
        User store2User = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("vendor_owner")
                        .password("password")
                        .name("상점2 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );

        Vendor store2Vendor = newVendor(store2);
        OrderTemplate templateOfStore2 = newOrderTemplate(store2Vendor);

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                "제목",
                "본문",
                true
        );

        // when & then
        assertThatThrownBy(() ->
                orderTemplateService.updateOrderTemplate(templateOfStore2.getId(), request, store1User.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_TEMPLATE_ACCESS_DENIED.getMessage());
    }

    @Test
    void 발주_템플릿_상세_조회에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Vendor vendor = newVendor(store);
        OrderTemplate template = newOrderTemplate(vendor);

        // when
        OrderTemplateResponse response =
                orderTemplateService.getOrderTemplateDetail(template.getId(), user.getId());

        // then
        assertThat(response.orderTemplateId()).isEqualTo(template.getId());
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(response.title()).isEqualTo("기존 제목");
        assertThat(response.body()).isEqualTo("기존 본문");
        assertThat(response.activated()).isTrue();
    }

    @Test
    void 사용자가_없으면_발주_템플릿_상세_조회시_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        // when & then
        assertThatThrownBy(() ->
                orderTemplateService.getOrderTemplateDetail(1L, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 템플릿이_존재하지_않으면_발주_템플릿_상세_조회시_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);
        Long notExistTemplateId = 9999L;

        // when & then
        assertThatThrownBy(() ->
                orderTemplateService.getOrderTemplateDetail(notExistTemplateId, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_TEMPLATE_NOT_FOUND.getMessage());
    }

    @Test
    void 다른_상점의_발주_템플릿이면_상세_조회시_접근이_거부된다() {
        // given
        Store store1 = newStore();
        Store store2 = newStore();

        User store1User = newUser(store1);
        User store2User = userRepository.save(
                User.builder()
                        .store(store2)
                        .username("vendor_owner")
                        .password("password")
                        .name("상점2 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );

        Vendor store2Vendor = newVendor(store2);
        OrderTemplate templateOfStore2 = newOrderTemplate(store2Vendor);

        // when & then
        assertThatThrownBy(() ->
                orderTemplateService.getOrderTemplateDetail(templateOfStore2.getId(), store1User.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.ORDER_TEMPLATE_ACCESS_DENIED.getMessage());
    }
}
