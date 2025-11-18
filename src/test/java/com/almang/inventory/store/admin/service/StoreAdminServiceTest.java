package com.almang.inventory.store.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.admin.dto.request.StoreAdminCreateRequest;
import com.almang.inventory.store.admin.dto.response.StoreAdminCreateResponse;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class StoreAdminServiceTest {

    @Autowired private StoreAdminService storeAdminService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Store newStore() {
        return storeRepository.save(
                Store.builder()
                        .name("테스트 상점")
                        .isActivate(true)
                        .defaultCountCheckThreshold(BigDecimal.valueOf(0.2))
                        .build()
        );
    }

    @Test
    void 상점_관리자_생성에_성공하면_응답을_반환한다() {
        // given
        Store store = newStore();
        StoreAdminCreateRequest request = new StoreAdminCreateRequest(
                "store_admin",
                "상점 관리자",
                store.getId()
        );

        // when
        StoreAdminCreateResponse response = storeAdminService.createStoreAdmin(request);

        // then
        assertThat(response.userId()).isNotNull();
        assertThat(response.username()).isEqualTo("store_admin");
        assertThat(response.name()).isEqualTo("상점 관리자");
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.password()).isNotBlank();
        assertThat(response.password().length()).isEqualTo(12);
    }

    @Test
    void 상점이_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistStoreId = 9999L;
        StoreAdminCreateRequest request = new StoreAdminCreateRequest(
                "admin_user",
                "관리자",
                notExistStoreId
        );

        // when & then
        assertThatThrownBy(() -> storeAdminService.createStoreAdmin(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.STORE_NOT_FOUND.getMessage());
    }

    @Test
    void 이미_존재하는_아이디로_상점_관리자를_생성하면_예외가_빨생한다() {
        // given
        Store store = newStore();
        String duplicatedUsername = "store_admin";

        userRepository.save(
                User.builder()
                        .store(store)
                        .username(duplicatedUsername)
                        .password(passwordEncoder.encode("password"))
                        .name("상점 관리자")
                        .role(UserRole.ADMIN)
                        .build()
        );

        StoreAdminCreateRequest request = new StoreAdminCreateRequest(
                duplicatedUsername,
                "새 관리자",
                store.getId()
        );

        // when & then
        assertThatThrownBy(() -> storeAdminService.createStoreAdmin(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_USERNAME.getMessage());
    }
}
