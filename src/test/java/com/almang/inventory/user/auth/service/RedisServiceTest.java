package com.almang.inventory.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class RedisServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private RedisService redisService;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(redisService, "refreshTokenExpiration", 7);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void 리프레시_토큰을_저장한다() {
        // given
        String userId = "1";
        String refreshToken = "token-value";

        // when
        redisService.saveRefreshToken(userId, refreshToken);

        // then
        verify(valueOperations).set("refresh:user:1", refreshToken, Duration.ofDays(7));
        verify(valueOperations).set("refresh:token:token-value", userId, Duration.ofDays(7));
    }

    @Test
    void userId로_리프레시_토큰을_조회한다() {
        // given
        String userId = "1";
        given(valueOperations.get("refresh:user:1")).willReturn("token-value");

        // when
        String result = redisService.getRefreshTokenByUserId(userId);

        // then
        assertThat(result).isEqualTo("token-value");
    }

    @Test
    void refreshToken으로_userId를_조회한다() {
        // given
        String token = "token-value";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:token:token-value")).willReturn("1");

        // when
        String result = redisService.getUserIdByRefreshToken(token);

        // then
        assertThat(result).isEqualTo("1");
    }

    @Test
    void userId로_리프레시_토큰을_삭제한다() {
        // given
        String userId = "1";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:user:1")).willReturn("token-value");

        // when
        redisService.deleteByUserId(userId);

        // then
        verify(redisTemplate).delete("refresh:token:token-value");
        verify(redisTemplate).delete("refresh:user:1");
    }

    @Test
    void refreshToken으로_리프레시_토큰을_삭제한다() {
        // given
        String token = "token-value";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:token:token-value")).willReturn("1");

        // when
        redisService.deleteByRefreshToken(token);

        // then
        verify(redisTemplate).delete("refresh:user:1");
        verify(redisTemplate).delete("refresh:token:token-value");
    }

    @Test
    void 기존_리프레시_토큰이_존재할_때_리프레시_토큰을_rotate한다() {
        // given
        String userId = "1";
        String oldToken = "old-token";
        String newToken = "new-token";

        // when
        redisService.rotateRefreshToken(userId, oldToken, newToken);

        // then
        verify(redisTemplate).delete("refresh:token:old-token");
        verify(redisTemplate).delete("refresh:user:1");
        verify(valueOperations).set("refresh:user:1", newToken, Duration.ofDays(7));
        verify(valueOperations).set("refresh:token:new-token", userId, Duration.ofDays(7));
    }

    @Test
    void userId와_refreshToken으로_리프레시_토큰을_삭제한다() {
        // given
        String userId = "1";
        String refreshToken = "token-value";

        // when
        redisService.deleteRefreshToken(userId, refreshToken);

        // then
        verify(redisTemplate).delete("refresh:user:1");
        verify(redisTemplate).delete("refresh:token:token-value");
    }

    @Test
    void 액세스_토큰을_블랙리스트에_추가한다() {
        // given
        String accessToken = "access-token";
        long remainingMillis = 1000L;

        // when
        redisService.addAccessTokenToBlacklist(accessToken, remainingMillis);

        // then
        verify(valueOperations).set("blacklist:access:access-token", "true", Duration.ofMillis(remainingMillis));
    }

    @Test
    void 만료시간이_0이하면_블랙리스트에_추가하지_않는다() {
        // given
        String accessToken = "access-token";
        long remainingMillis = 0L;

        // when
        redisService.addAccessTokenToBlacklist(accessToken, remainingMillis);

        // then
        verifyNoInteractions(valueOperations);
    }

    @Test
    void 액세스_토큰이_블랙리스트에_포함되어_있으면_true를_반환한다() {
        // given
        String accessToken = "access-token";
        given(redisTemplate.hasKey("blacklist:access:access-token")).willReturn(true);

        // when
        boolean result = redisService.isAccessTokenBlacklisted(accessToken);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 액세스_토큰이_블랙리스트에_포함되어_있지_않으면_false를_반환한다() {
        // given
        String accessToken = "access-token";
        given(redisTemplate.hasKey("blacklist:access:access-token")).willReturn(false);

        // when
        boolean result = redisService.isAccessTokenBlacklisted(accessToken);

        // then
        assertThat(result).isFalse();
    }
}
