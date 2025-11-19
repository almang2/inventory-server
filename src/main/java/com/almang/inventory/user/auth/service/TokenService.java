package com.almang.inventory.user.auth.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${auth.refresh.expiration-days}")
    private int refreshTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "refreshToken";
    private static final String ACCESS_TOKEN_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    public String issueAccessToken(Long userId) {
        return jwtTokenProvider.generateAccessToken(userId);
    }

    public void issueRefreshToken(Long userId, HttpServletResponse response) {
        String refreshToken = UUID.randomUUID().toString();
        redisService.saveRefreshToken(userId.toString(), refreshToken);

        setRefreshTokenCookie(response, refreshToken);
    }

    public String reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);

        String userId = redisService.getUserIdByRefreshToken(refreshToken);

        if (userId == null) {
            throw new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        String newRefreshToken = UUID.randomUUID().toString();
        redisService.rotateRefreshToken(userId, refreshToken, newRefreshToken);

        setRefreshTokenCookie(response, newRefreshToken);

        return issueAccessToken(Long.parseLong(userId));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_PREFIX, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(refreshTokenExpiration))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            throw new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        return Arrays.stream(cookies)
                .filter(cookie -> REFRESH_TOKEN_PREFIX.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    public void revokeTokens(HttpServletRequest request, HttpServletResponse response, Long userId) {
        String accessToken = resolveToken(request);

        // 액세스 토큰 무효화
        if (accessToken != null && !accessToken.isBlank()) {
            long remainMillis = jwtTokenProvider.getRemainingMillis(accessToken);
            if (remainMillis > 0) {
                redisService.addAccessTokenToBlacklist(accessToken, remainMillis);
            }
        }

        // 리프레시 토큰 삭제
        redisService.deleteByUserId(userId.toString());

        // 리프레시 쿠키 삭제
        clearRefreshTokenCookie(response);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_PREFIX, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearer != null && bearer.startsWith(ACCESS_TOKEN_PREFIX)) {
            return bearer.substring(ACCESS_TOKEN_PREFIX.length());
        }
        return null;
    }
}
