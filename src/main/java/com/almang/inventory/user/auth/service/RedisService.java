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
    private static final String ACCESS_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String CAFE24_ACCESS_TOKEN_KEY = "cafe24:access_token";
    private static final String CAFE24_OAUTH_STATE_PREFIX = "cafe24:oauth:state:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.refresh.expiration-days}")
    private int refreshTokenExpiration;

    private String userKey(String userId) {
        return REFRESH_USER_PREFIX + userId;
    }

    private String tokenKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }

    private String blacklistAccessTokenKey(String token) {
        return ACCESS_BLACKLIST_PREFIX + token;
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

    public void rotateRefreshToken(String userId, String oldToken, String newToken) {
        if (oldToken != null) {
            redisTemplate.delete(tokenKey(oldToken));
        }
        redisTemplate.delete(userKey(userId));
        saveRefreshToken(userId, newToken);
    }

    public void addAccessTokenToBlacklist(String accessToken, long remainingMillis) {
        if (remainingMillis <= 0) return;
        redisTemplate.opsForValue().set(
                blacklistAccessTokenKey(accessToken), "true", Duration.ofMillis(remainingMillis));
    }

    public boolean isAccessTokenBlacklisted(String accessToken) {
        return redisTemplate.hasKey(blacklistAccessTokenKey(accessToken));
    }

    /**
     * 카페24 Access Token을 Redis에 저장합니다.
     * 
     * @param accessToken 카페24 Access Token
     * @param expiresInSeconds 만료 시간 (초 단위)
     */
    public void saveCafe24AccessToken(String accessToken, long expiresInSeconds) {
        // 만료 시간을 약간 여유있게 설정 (예: 5분 전에 만료되도록)
        long ttlSeconds = Math.max(expiresInSeconds - 300, 60); // 최소 1분은 보장
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        
        redisTemplate.opsForValue().set(CAFE24_ACCESS_TOKEN_KEY, accessToken, ttl);
    }

    /**
     * 저장된 카페24 Access Token을 조회합니다.
     * 
     * @return 카페24 Access Token (없으면 null)
     */
    public String getCafe24AccessToken() {
        return redisTemplate.opsForValue().get(CAFE24_ACCESS_TOKEN_KEY);
    }

    /**
     * 저장된 카페24 Access Token을 삭제합니다.
     */
    public void deleteCafe24AccessToken() {
        redisTemplate.delete(CAFE24_ACCESS_TOKEN_KEY);
    }

    /**
     * 카페24 OAuth state 값을 Redis에 저장합니다.
     * (CSRF 방지를 위한 state 값, 10분 후 자동 만료)
     * 
     * @param state OAuth state 값
     */
    public void saveCafe24OAuthState(String state) {
        Duration ttl = Duration.ofMinutes(10); // 10분 후 자동 만료
        redisTemplate.opsForValue().set(CAFE24_OAUTH_STATE_PREFIX + state, "true", ttl);
    }

    /**
     * 카페24 OAuth state 값이 Redis에 존재하는지 확인합니다.
     * 
     * @param state OAuth state 값
     * @return state가 존재하면 true, 없으면 false
     */
    public boolean hasCafe24OAuthState(String state) {
        if (state == null || state.isEmpty()) {
            return false;
        }
        return redisTemplate.hasKey(CAFE24_OAUTH_STATE_PREFIX + state);
    }

    /**
     * 카페24 OAuth state 값을 Redis에서 삭제합니다.
     * (일회성 보장을 위해 사용 후 즉시 삭제)
     * 
     * @param state OAuth state 값
     */
    public void deleteCafe24OAuthState(String state) {
        if (state != null && !state.isEmpty()) {
            redisTemplate.delete(CAFE24_OAUTH_STATE_PREFIX + state);
        }
    }
}
