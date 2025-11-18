package com.almang.inventory.store.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.almang.inventory.store.admin.dto.request.StoreAdminCreateRequest;
import com.almang.inventory.store.admin.dto.response.StoreAdminCreateResponse;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.repository.UserRepository;
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
                        .defaultCountCheckThreshold(0.2)
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
}
