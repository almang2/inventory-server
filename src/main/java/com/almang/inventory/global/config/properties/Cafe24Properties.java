package com.almang.inventory.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("cafe24.api") // .env 파일의 cafe24.api 접두사를 가진 변수들을 바인딩
public class Cafe24Properties {

    private String oauthUrl; // CAFE24_API_OAUTH_URL
    private String baseUrl; // CAFE24_API_BASE_URL
    private String clientId; // CAFE24_API_CLIENT_ID
    private String clientSecret; // CAFE24_API_CLIENT_SECRET
    private String redirectUri; // CAFE24_API_REDIRECT_URI
    private String scope; // CAFE24_API_SCOPE
    private String oauthSuccessRedirectUrl; // CAFE24_OAUTH_SUCCESS_REDIRECT_URL
}
