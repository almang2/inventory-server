package com.almang.inventory.cafe24.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cafe24_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cafe24Token {

    @Id
    private String mallId;

    @Column(length = 1000)
    private String accessToken;

    @Column(length = 1000)
    private String refreshToken;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime refreshTokenExpiresAt;

    public Cafe24Token(String mallId, String accessToken, String refreshToken, LocalDateTime issuedAt,
            LocalDateTime expiresAt, LocalDateTime refreshTokenExpiresAt) {
        this.mallId = mallId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void updateToken(String accessToken, String refreshToken, LocalDateTime issuedAt, LocalDateTime expiresAt,
            LocalDateTime refreshTokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }
}
