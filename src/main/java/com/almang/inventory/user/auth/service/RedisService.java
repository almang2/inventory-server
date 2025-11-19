package com.almang.inventory.user.auth.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String REFRESH_USER_PREFIX = "refresh:user:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.refresh.expiration-days}")
    private int refreshTokenExpiration;

    private String userKey(String userId) {
        return REFRESH_USER_PREFIX + userId;
    }

    private String tokenKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }

    public void saveRefreshToken(String userId, String refreshToken) {
        Duration ttl = Duration.ofDays(refreshTokenExpiration);

        redisTemplate.opsForValue().set(userKey(userId), refreshToken, ttl);
        redisTemplate.opsForValue().set(tokenKey(refreshToken), userId, ttl);
    }

    public String getRefreshTokenByUserId(String userId) {
        return redisTemplate.opsForValue().get(userKey(userId));
    }

    public String getUserIdByRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(tokenKey(refreshToken));
    }

    public void deleteByUserId(String userId) {
        String refreshToken = getRefreshTokenByUserId(userId);
        if (refreshToken != null) {
            redisTemplate.delete(tokenKey(refreshToken));
        }
        redisTemplate.delete(userKey(userId));
    }

    public void deleteByRefreshToken(String refreshToken) {
        String userId = getUserIdByRefreshToken(refreshToken);
        if (userId != null) {
            redisTemplate.delete(userKey(userId));
        }
        redisTemplate.delete(tokenKey(refreshToken));
    }

    public void deleteRefreshToken(String userId, String refreshToken) {
        redisTemplate.delete(userKey(userId));
        redisTemplate.delete(tokenKey(refreshToken));
    }
}
