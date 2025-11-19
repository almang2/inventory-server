package com.almang.inventory.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisService redisService;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpServletResponse httpServletResponse;

    @InjectMocks private TokenService tokenService;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", 7);
    }

    @Test
    void 액세스_토큰을_발급한다() {
        // given
        Long userId = 1L;
        String expectedToken = "access-token";
        org.mockito.BDDMockito.given(jwtTokenProvider.generateAccessToken(userId))
                .willReturn(expectedToken);

        // when
        String result = tokenService.issueAccessToken(userId);

        // then
        assertThat(result).isEqualTo(expectedToken);
        verify(jwtTokenProvider).generateAccessToken(userId);
    }

    @Test
    void 리프레시_토큰을_발급하고_redis에_저장하며_쿠키를_설정한다() {
        // given
        Long userId = 1L;

        // when
        tokenService.issueRefreshToken(userId, httpServletResponse);

        // then
        verify(redisService).saveRefreshToken(eq("1"), anyString());

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        String cookieValue = headerCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=");
        assertThat(cookieValue).contains("Path=/");
        assertThat(cookieValue).contains("HttpOnly");
        assertThat(cookieValue).contains("Secure");
        assertThat(cookieValue).contains("SameSite=None");
        assertThat(cookieValue).contains("Max-Age=" + Duration.ofDays(7).toSeconds());
    }

    @Test
    void 리프레시_토큰으로_새_액세스_토큰과_새_리프레시_토큰을_발급한다() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String userId = "1";
        String newAccessToken = "new-access-token";

        Cookie refreshCookie = new Cookie("refreshToken", oldRefreshToken);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[] {refreshCookie});
        when(redisService.getUserIdByRefreshToken(oldRefreshToken)).thenReturn(userId);
        when(jwtTokenProvider.generateAccessToken(1L)).thenReturn(newAccessToken);

        // when
        String result = tokenService.reissueAccessToken(httpServletRequest, httpServletResponse);

        // then
        // 액세스 토큰 재발급 확인
        assertThat(result).isEqualTo(newAccessToken);
        verify(jwtTokenProvider).generateAccessToken(1L);

        // 리프레시 토큰 rotate 확인
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> oldTokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newTokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisService).rotateRefreshToken(
                userIdCaptor.capture(), oldTokenCaptor.capture(), newTokenCaptor.capture()
        );

        assertThat(userIdCaptor.getValue()).isEqualTo("1");
        assertThat(oldTokenCaptor.getValue()).isEqualTo(oldRefreshToken);
        assertThat(newTokenCaptor.getValue()).isNotNull();

        // 새 리프레시 토큰이 쿠키에 설정되는지 확인
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        String cookieValue = headerCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=");
        assertThat(cookieValue).contains("HttpOnly");
        assertThat(cookieValue).contains("Secure");
        assertThat(cookieValue).contains("SameSite=None");
    }

    @Test
    void 쿠키에_refreshToken이_없으면_재발급_시_예외가_발생한다() {
        // given
        when(httpServletRequest.getCookies()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> tokenService.reissueAccessToken(httpServletRequest, httpServletResponse))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    void redis에_refreshToken이_존재하지_않으면_재발급_시_예외가_발생한다() {
        // given
        String refreshToken = "invalid-refresh-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[] {cookie});
        when(redisService.getUserIdByRefreshToken(refreshToken)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> tokenService.reissueAccessToken(httpServletRequest, httpServletResponse))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    void 액세스_토큰과_리프레시_토큰을_모두_폐기한다() {
        // given
        Long userId = 1L;
        String accessToken = "access-token";

        when(httpServletRequest.getHeader("Authorization"))
                .thenReturn("Bearer " + accessToken);
        when(jwtTokenProvider.getRemainingMillis(accessToken))
                .thenReturn(1000L);

        // when
        tokenService.revokeTokens(httpServletRequest, httpServletResponse, userId);

        // then
        // 액세스 토큰 블랙리스트 등록
        verify(jwtTokenProvider).getRemainingMillis(accessToken);
        verify(redisService).addAccessTokenToBlacklist(accessToken, 1000L);

        // 리프레시 토큰 삭제
        verify(redisService).deleteByUserId("1");

        // 리프레시 토큰 쿠키 삭제(Set-Cookie 헤더 확인)
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        String cookieValue = headerCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=");
        assertThat(cookieValue).contains("Max-Age=0");
        assertThat(cookieValue).contains("Path=/");
        assertThat(cookieValue).contains("HttpOnly");
        assertThat(cookieValue).contains("Secure");
        assertThat(cookieValue).contains("SameSite=None");
    }

    @Test
    void Authorization_헤더가_없어도_리프레시_토큰은_삭제된다() {
        // given
        Long userId = 1L;
        when(httpServletRequest.getHeader("Authorization"))
                .thenReturn(null);

        // when
        tokenService.revokeTokens(httpServletRequest, httpServletResponse, userId);

        // then
        verify(redisService).deleteByUserId("1");

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        String cookieValue = headerCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=");
        assertThat(cookieValue).contains("Max-Age=0");
    }

    @Test
    void 액세스_토큰_남은_시간이_없으면_블랙리스트에_추가하지_않는다() {
        // given
        Long userId = 1L;
        String accessToken = "access-token";

        when(httpServletRequest.getHeader("Authorization"))
                .thenReturn("Bearer " + accessToken);
        when(jwtTokenProvider.getRemainingMillis(accessToken))
                .thenReturn(0L);

        // when
        tokenService.revokeTokens(httpServletRequest, httpServletResponse, userId);

        // then
        verify(jwtTokenProvider).getRemainingMillis(accessToken);
        verify(redisService, org.mockito.Mockito.never())
                .addAccessTokenToBlacklist(anyString(), org.mockito.Mockito.anyLong());
        verify(redisService).deleteByUserId("1");
    }
}
