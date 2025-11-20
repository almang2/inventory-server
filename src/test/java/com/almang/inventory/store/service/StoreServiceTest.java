package com.almang.inventory.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.dto.request.UpdateStoreRequest;
import com.almang.inventory.store.dto.response.UpdateStoreResponse;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class StoreServiceTest {

    @Autowired private StoreService storeService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;

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
                        .username("tester")
                        .password("encoded-password")
                        .name("테스트 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );
    }

    @Test
    void 상점_이름_수정에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        UpdateStoreRequest request = new UpdateStoreRequest(
                "수정된 상점 이름",
                null,
                null
        );

        // when
        UpdateStoreResponse response = storeService.updateStore(request, user.getId());

        // then
        Store updatedStore = storeRepository.findById(store.getId())
                .orElseThrow();

        assertThat(response).isNotNull();
        assertThat(updatedStore.getName()).isEqualTo("수정된 상점 이름");
        assertThat(updatedStore.getDefaultCountCheckThreshold()).isEqualByComparingTo("0.2");
        assertThat(updatedStore.isActivate()).isTrue();
    }

    @Test
    void 상점_임계치_수정에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        BigDecimal newThreshold = BigDecimal.valueOf(0.5);

        UpdateStoreRequest request = new UpdateStoreRequest(
                null,
                newThreshold,
                null
        );

        // when
        UpdateStoreResponse response = storeService.updateStore(request, user.getId());

        // then
        Store updatedStore = storeRepository.findById(store.getId())
                .orElseThrow();

        assertThat(response).isNotNull();
        assertThat(updatedStore.getDefaultCountCheckThreshold()).isEqualByComparingTo(newThreshold);
        assertThat(updatedStore.getName()).isEqualTo("테스트 상점");
        assertThat(updatedStore.isActivate()).isTrue();
    }

    @Test
    void 상점_활성화_여부_수정에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        UpdateStoreRequest request = new UpdateStoreRequest(
                null,
                null,
                false
        );

        // when
        UpdateStoreResponse response = storeService.updateStore(request, user.getId());

        // then
        Store updatedStore = storeRepository.findById(store.getId())
                .orElseThrow();

        assertThat(response).isNotNull();
        assertThat(updatedStore.isActivate()).isFalse();
        assertThat(updatedStore.getName()).isEqualTo("테스트 상점");
        assertThat(updatedStore.getDefaultCountCheckThreshold()).isEqualByComparingTo("0.2");
    }

    @Test
    void 상점_정보_여러_항목을_동시_수정에_성공한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        BigDecimal newThreshold = BigDecimal.valueOf(0.7);

        UpdateStoreRequest request = new UpdateStoreRequest(
                "동시 수정 상점",
                newThreshold,
                false
        );

        // when
        UpdateStoreResponse response = storeService.updateStore(request, user.getId());

        // then
        Store updatedStore = storeRepository.findById(store.getId())
                .orElseThrow();

        assertThat(response).isNotNull();
        assertThat(updatedStore.getName()).isEqualTo("동시 수정 상점");
        assertThat(updatedStore.getDefaultCountCheckThreshold()).isEqualByComparingTo(newThreshold);
        assertThat(updatedStore.isActivate()).isFalse();
    }

    @Test
    void 상점_정보_수정_시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        UpdateStoreRequest request = new UpdateStoreRequest(
                "아무거나",
                BigDecimal.valueOf(0.3),
                true
        );

        // when & then
        assertThatThrownBy(() -> storeService.updateStore(request, notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 상점_이름이_20자를_초과하면_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        String longName = "123456789012345678901"; // 21자
        UpdateStoreRequest request = new UpdateStoreRequest(
                longName,
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> storeService.updateStore(request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_NAME_IS_LONG.getMessage());
    }

    @Test
    void 상점_임계치가_0보다_작으면_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        UpdateStoreRequest request = new UpdateStoreRequest(
                null,
                BigDecimal.valueOf(-0.1),
                null
        );

        // when & then
        assertThatThrownBy(() -> storeService.updateStore(request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.DEFAULT_COUNT_CHECK_THRESHOLD_NOT_IN_RANGE.getMessage());
    }

    @Test
    void 상점_임계치가_1보다_크면_예외가_발생한다() {
        // given
        Store store = newStore();
        User user = newUser(store);

        UpdateStoreRequest request = new UpdateStoreRequest(
                null,
                BigDecimal.valueOf(1.1),
                null
        );

        // when & then
        assertThatThrownBy(() -> storeService.updateStore(request, user.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.DEFAULT_COUNT_CHECK_THRESHOLD_NOT_IN_RANGE.getMessage());
    }
}
