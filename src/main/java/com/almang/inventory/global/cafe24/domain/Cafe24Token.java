package com.almang.inventory.global.cafe24.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cafe24_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cafe24Token extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "mall_id", nullable = false, length = 50)
    private String mallId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "shop_no", length = 20)
    private String shopNo;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public void updateTokens(String accessToken, String refreshToken, 
                             LocalDateTime expiresAt, LocalDateTime refreshTokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isAccessTokenExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRefreshTokenExpired() {
        return LocalDateTime.now().isAfter(refreshTokenExpiresAt);
    }
}

