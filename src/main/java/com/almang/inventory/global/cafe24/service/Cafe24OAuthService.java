package com.almang.inventory.global.cafe24.service;

import com.almang.inventory.global.cafe24.dto.Cafe24TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.logging.LogLevel;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class Cafe24OAuthService {

    private final WebClient webClient;

    @Value("${cafe24.api.oauth-url}")
    private String oauthBaseUrl;

    @Value("${cafe24.api.client-id}")
    private String clientId;

    @Value("${cafe24.api.client-secret}")
    private String clientSecret;

    @Value("${cafe24.api.redirect-uri}")
    private String redirectUri;

    @Value("${cafe24.api.scope}")
    private String scope;

    public Cafe24OAuthService(WebClient.Builder webClientBuilder,
                              @Value("${cafe24.api.oauth-url}") String oauthBaseUrl) {
        HttpClient httpClient = HttpClient.create()
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(oauthBaseUrl)
                .build();
        this.oauthBaseUrl = oauthBaseUrl;
    }

    public String generateAuthorizationUrl() {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);

        return String.format("%sapi/v2/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s",
                oauthBaseUrl, clientId, encodedRedirectUri, encodedScope);
    }

    public String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);
        formData.add("scope", scope);

        log.debug("Cafe24 Access Token Request - Form Data: {}", formData);

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        log.debug("Cafe24 Access Token Request - Authorization Header: {}", authHeader);

        Cafe24TokenResponse tokenResponse = webClient.post()
                .uri("api/v2/oauth/token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("카페24 Access Token 발급 API 호출 실패 - 상태 코드: {}, 응답 본문: {}", clientResponse.statusCode(), errorBody);
                                return Mono.error(new RuntimeException("카페24 Access Token 발급 API 호출 실패"));
                            });
                })
                .bodyToMono(Cafe24TokenResponse.class)
                .block();

        if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
            // TODO: Access Token 및 Refresh Token을 저장하는 로직 구현
            log.info("Access Token 발급 성공: {}", tokenResponse.getAccessToken());
            return tokenResponse.getAccessToken();
        } else {
            log.error("Access Token 발급 실패: {}", tokenResponse);
            throw new RuntimeException("Failed to obtain Access Token from Cafe24.");
        }
    }
}
