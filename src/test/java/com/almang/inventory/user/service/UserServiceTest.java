package com.almang.inventory.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.dto.request.UpdateUserProfileRequest;
import com.almang.inventory.user.dto.response.DeleteUserResponse;
import com.almang.inventory.user.dto.response.UpdateUserProfileResponse;
import com.almang.inventory.user.dto.response.UserProfileResponse;
import com.almang.inventory.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
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

    private User newUser(Store store) {
        return userRepository.save(
                User.builder()
                        .store(store)
                        .username("tester")
                        .password(passwordEncoder.encode("password"))
                        .name("테스트 유저")
                        .role(UserRole.ADMIN)
                        .build()
        );
    }

    @Test
    void 사용자_정보_조회에_성공한다() {
        // given
        Store store = newStore();
        User savedUser = newUser(store);

        // when
        UserProfileResponse response = userService.getUserProfile(savedUser.getId());

        // then
        assertThat(response.username()).isEqualTo("tester");
        assertThat(response.name()).isEqualTo("테스트 유저");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.storeName()).isEqualTo("테스트 상점");
    }

    @Test
    void 사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;

        // when & then
        assertThatThrownBy(() -> userService.getUserProfile(notExistUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 사용자_정보_수정에_성공이다() {
        // given
        Store store = newStore();
        User savedUser = newUser(store);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("수정된 이름");

        // when
        UpdateUserProfileResponse response = userService.updateUserProfile(savedUser.getId(), request);

        // then
        assertThat(response).isNotNull();

        User updatedUser = userRepository.findById(savedUser.getId())
                .orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("수정된 이름");
    }

    @Test
    void 사용자_정보_수정_시_사용자를_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("수정된 이름");

        // when & then
        assertThatThrownBy(() -> userService.updateUserProfile(notExistUserId, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 사용자_정보_수정_시_이름이_20자를_초과하면_예외가_발생한다() {
        // given
        Store store = newStore();
        User savedUser = newUser(store);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("123456789012345678901");

        // when & then
        assertThatThrownBy(() -> userService.updateUserProfile(savedUser.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.NAME_IS_LONG.getMessage());
    }

    @Test
    void 회원_탈퇴에_성공한다() {
        // given
        Store store = newStore();
        User savedUser = newUser(store);

        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        // when
        DeleteUserResponse deleteResponse = userService.deleteUser(savedUser.getId(), request, response);

        // then
        assertThat(deleteResponse.success()).isTrue();

        User deletedUser = userRepository.findById(savedUser.getId())
                .orElseThrow();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
    }

    @Test
    void 회원_탈퇴_시_사용자가_존재하지_않으면_예외가_발생한다() {
        // given
        Long notExistUserId = 9999L;
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(notExistUserId, request, response))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }
}
