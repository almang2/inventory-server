package com.almang.inventory.store.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.almang.inventory.global.security.jwt.JwtTokenProvider;
import com.almang.inventory.global.security.jwt.TokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JwtTokenProviderTest {

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF"; // 32바이트 이상
    private static final int EXP_MINUTES = 10;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXP_MINUTES);
    }

    @Test
    void 유효한_토큰을_생성한다() {
        // given
        Long userId = 1L;

        // when
        String token = jwtTokenProvider.generateAccessToken(userId);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void 유효한_토큰_생성_시에_Claims에_userId가_들어간다() {
        // given
        Long userId = 1L;

        // when
        String token = jwtTokenProvider.generateAccessToken(userId);
        Long parsedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // then
        assertThat(parsedUserId).isEqualTo(userId);
    }

    @Test
    void 토큰이_유효할_때_VALID를_반환한다() {
        // given
        Long userId = 1L;
        String token = jwtTokenProvider.generateAccessToken(userId);

        // when
        TokenStatus status = jwtTokenProvider.validateToken(token);

        // then
        assertThat(status).isEqualTo(TokenStatus.VALID);
    }

    @Test
    void 토큰이_만료_되었을_때_EXPIRED를_반환한다() {
        // given
        JwtTokenProvider immediateExpireJwtTokenProvider = new JwtTokenProvider(SECRET, 0);
        Long userId = 1L;

        // when
        String token = immediateExpireJwtTokenProvider.generateAccessToken(userId);
        TokenStatus status = immediateExpireJwtTokenProvider.validateToken(token);

        // then
        assertThat(status).isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    void 유효하지_않은_토큰일_때_INVALID를_반환한다() {
        // when
        TokenStatus status = jwtTokenProvider.validateToken("strange-token");

        // then
        assertThat(status).isEqualTo(TokenStatus.INVALID);
    }

    @Test
    void 액세스_토큰에서_remaining_millis를_추출한다() {
        // given
        Long userId = 10L;

        // when
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        long remainingMillis = jwtTokenProvider.getRemainingMillis(accessToken);

        // then
        assertThat(remainingMillis).isNotEqualTo(0L);
    }
}
