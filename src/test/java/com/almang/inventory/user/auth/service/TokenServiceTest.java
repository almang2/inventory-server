package com.almang.inventory.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.almang.inventory.global.security.jwt.JwtTokenProvider;
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
}
