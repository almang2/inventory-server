package com.almang.inventory.user.auth.service;

import com.almang.inventory.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${auth.refresh.expiration-days}")
    private int refreshTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "refreshToken";

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
}
