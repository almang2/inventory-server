package com.almang.inventory.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.admin.dto.request.CreateStoreRequest;
import com.almang.inventory.admin.dto.response.CreateStoreResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    void 상점_생성에_성공하면_상점_정보를_반환한다() {
        // given
        CreateStoreRequest request = new CreateStoreRequest(
                "테스트 상점",
                BigDecimal.valueOf(0.2)
        );

        // when
        CreateStoreResponse response = adminService.createStore(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.storeId()).isNotNull();
        assertThat(response.name()).isEqualTo("테스트 상점");
        assertThat(response.defaultCountCheckThreshold()).isEqualByComparingTo("0.2");

        Store saved = storeRepository.findById(response.storeId())
                .orElseThrow();
        assertThat(saved.getName()).isEqualTo("테스트 상점");
        assertThat(saved.getDefaultCountCheckThreshold()).isEqualByComparingTo("0.2");
        assertThat(saved.isActivate()).isTrue();
    }

    @Test
    void 상점명이_20자를_초과하면_예외가_발생한다() {
        // given
        String longName = "123456789012345678901";
        CreateStoreRequest request = new CreateStoreRequest(
                longName,
                BigDecimal.valueOf(0.2)
        );

        // when & then
        assertThatThrownBy(() -> adminService.createStore(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_NAME_IS_LONG.getMessage());
    }

    @Test
    void 수량_점검_임계치가_0보다_작으면_예외가_발생한다() {
        // given
        CreateStoreRequest request = new CreateStoreRequest(
                "테스트 상점",
                BigDecimal.valueOf(-0.1)
        );

        // when & then
        assertThatThrownBy(() -> adminService.createStore(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.DEFAULT_COUNT_CHECK_THRESHOLD_NOT_IN_RANGE.getMessage());
    }

    @Test
    void 수량_점검_임계치가_1보다_크면_예외가_발생한다() {
        // given
        CreateStoreRequest request = new CreateStoreRequest(
                "테스트 상점",
                BigDecimal.valueOf(1.1)
        );

        // when & then
        assertThatThrownBy(() -> adminService.createStore(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.DEFAULT_COUNT_CHECK_THRESHOLD_NOT_IN_RANGE.getMessage());
    }
}