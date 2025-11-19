package com.almang.inventory.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.user.auth.dto.request.ChangePasswordRequest;
import com.almang.inventory.user.auth.dto.request.LoginRequest;
import com.almang.inventory.user.auth.dto.request.ResetPasswordRequest;
import com.almang.inventory.user.auth.dto.response.ChangePasswordResponse;
import com.almang.inventory.user.auth.dto.response.LoginResponse;
import com.almang.inventory.user.auth.dto.response.LogoutResponse;
import com.almang.inventory.user.auth.dto.response.ResetPasswordResponse;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private TokenService tokenService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpServletResponse httpServletResponse;

    @InjectMocks private AuthService authService;

    @Test
    void 로그인_성공하면_액세스_토큰을_반환한다() {
        // given
        String username = "store_admin";
        String password = "password";
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .username(username)
                .password("encoded-password")
                .build();

        LoginRequest request = new LoginRequest(username, password);

        given(userRepository.findByUsername(username))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword()))
                .willReturn(true);
        given(tokenService.issueAccessToken(userId))
                .willReturn("access-token");

        // when
        LoginResponse response = authService.login(request, httpServletResponse);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(tokenService).issueAccessToken(userId);
        verify(tokenService).issueRefreshToken(userId, httpServletResponse);
    }

    @Test
    void 존재하지_않는_username으로_로그인하면_예외가_발생한다() {
        // given
        String username = "store_admin";
        LoginRequest request = new LoginRequest(username, "password");

        given(userRepository.findByUsername(username))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request, httpServletResponse))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 비밀번호가_일치하지_않으면_예외가_발생한다() {
        // given
        String username = "store_admin";
        String password = "error-password";

        User user = User.builder()
                .id(1L)
                .username(username)
                .password("encoded-password")
                .build();

        LoginRequest request = new LoginRequest(username, password);

        given(userRepository.findByUsername(username))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword()))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request, httpServletResponse))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    void 비밀번호_변경에_성공하면_true를_반환한다() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .username("store_admin")
                .password("encoded-password")
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("new-password");

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));
        given(passwordEncoder.encode("new-password"))
                .willReturn("encoded-new-password");

        // when
        ChangePasswordResponse response = authService.changePassword(request, userId);

        // then
        assertThat(response.isChanged()).isTrue();
        verify(passwordEncoder).encode("new-password");
        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
    }

    @Test
    void 비밀번호_변경_시_사용자를_찾지_못하면_예외가_발생한다() {
        // given
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("new-password");

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.changePassword(request, userId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void 로그아웃에_성공하면_true를_반환하고_토큰을_폐기한다() {
        // given
        Long userId = 1L;

        // when
        LogoutResponse response = authService.logout(userId, httpServletRequest, httpServletResponse);

        // then
        assertThat(response.success()).isTrue();
        verify(tokenService).revokeTokens(httpServletRequest, httpServletResponse, userId);
    }

    @Test
    void 비밀번호_초기화에_성공하면_임시_비밀번호를_반환한다() {
        // given
        String username = "store_admin";
        User user = User.builder()
                .id(1L)
                .username(username)
                .password("old-password")
                .build();

        ResetPasswordRequest request = new ResetPasswordRequest(username);

        given(userRepository.findByUsername(username))
                .willReturn(Optional.of(user));
        given(passwordEncoder.encode(anyString()))
                .willReturn("encoded-random-password");

        // when
        ResetPasswordResponse response = authService.resetPassword(request);

        // then
        ArgumentCaptor<String> rawPasswordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawPasswordCaptor.capture());
        String randomPassword = rawPasswordCaptor.getValue();

        assertThat(response.password()).isEqualTo(randomPassword);
        assertThat(randomPassword).isNotBlank();
        assertThat(user.getPassword()).isEqualTo("encoded-random-password");
    }

    @Test
    void 비밀번호_초기화_시_사용자를_찾지_못하면_예외가_발생한다() {
        // given
        String username = "unknown-user";
        ResetPasswordRequest request = new ResetPasswordRequest(username);

        given(userRepository.findByUsername(username))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }
}
