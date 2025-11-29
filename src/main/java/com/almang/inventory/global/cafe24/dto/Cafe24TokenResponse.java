package com.almang.inventory.global.cafe24.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Cafe24TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime expiresAt;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_token_expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime refreshTokenExpiresAt;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("mall_id")
    private String mallId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("scopes")
    private List<String> scopes;

    @JsonProperty("issued_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime issuedAt;

    @JsonProperty("shop_no")
    private String shopNo;
}
