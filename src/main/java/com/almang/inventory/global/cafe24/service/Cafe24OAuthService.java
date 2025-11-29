package com.almang.inventory.global.cafe24.service;

import com.almang.inventory.global.cafe24.domain.Cafe24Token;
import com.almang.inventory.global.cafe24.dto.Cafe24TokenResponse;
import com.almang.inventory.global.cafe24.repository.Cafe24TokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final Cafe24TokenRepository cafe24TokenRepository;

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
                              @Value("${cafe24.api.oauth-url}") String oauthBaseUrl,
                              Cafe24TokenRepository cafe24TokenRepository) {
        HttpClient httpClient = HttpClient.create()
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(oauthBaseUrl)
                .build();
        this.oauthBaseUrl = oauthBaseUrl;
        this.cafe24TokenRepository = cafe24TokenRepository;
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
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

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
                                log.error("요청 파라미터 - redirect_uri: {}, code: {}", redirectUri, code);
                                return Mono.error(new RuntimeException("카페24 Access Token 발급 실패: " + errorBody));
                            });
                })
                .bodyToMono(Cafe24TokenResponse.class)
                .block();

        if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
            saveTokens(tokenResponse);
            log.info("Access Token 발급 및 저장 성공: {}", tokenResponse.getAccessToken());
            return tokenResponse.getAccessToken();
        } else {
            log.error("Access Token 발급 실패: {}", tokenResponse);
            throw new RuntimeException("Failed to obtain Access Token from Cafe24.");
        }
    }

    @Transactional
    public void saveTokens(Cafe24TokenResponse tokenResponse) {
        String mallId = tokenResponse.getMallId();
        
        // 기존 토큰이 있으면 비활성화
        cafe24TokenRepository.findByMallIdAndActiveTrue(mallId)
                .ifPresent(existingToken -> {
                    existingToken.deactivate();
                    cafe24TokenRepository.save(existingToken);
                });

        // 새 토큰 저장
        Cafe24Token newToken = Cafe24Token.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .expiresAt(tokenResponse.getExpiresAt())
                .refreshTokenExpiresAt(tokenResponse.getRefreshTokenExpiresAt())
                .mallId(mallId)
                .userId(tokenResponse.getUserId())
                .shopNo(tokenResponse.getShopNo())
                .active(true)
                .build();

        cafe24TokenRepository.save(newToken);
        log.info("Cafe24 토큰 저장 완료 - mallId: {}, expiresAt: {}", mallId, tokenResponse.getExpiresAt());
    }

    public String getValidAccessToken() {
        // 활성 토큰 중 유효한 Access Token 찾기
        Cafe24Token token = cafe24TokenRepository.findAll().stream()
                .filter(Cafe24Token::isActive)
                .filter(t -> !t.isAccessTokenExpired())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("유효한 Cafe24 Access Token이 없습니다. OAuth 인증을 다시 진행해주세요."));
        
        return token.getAccessToken();
    }
    
    public String getValidAccessTokenOrRefresh() {
        // 활성 토큰 중 유효한 Access Token 찾기
        Cafe24Token token = cafe24TokenRepository.findAll().stream()
                .filter(Cafe24Token::isActive)
                .filter(t -> !t.isAccessTokenExpired())
                .findFirst()
                .orElse(null);
        
        if (token != null) {
            return token.getAccessToken();
        }
        
        // Access Token이 만료되었으면 Refresh Token으로 갱신 시도
        token = cafe24TokenRepository.findAll().stream()
                .filter(Cafe24Token::isActive)
                .filter(t -> !t.isRefreshTokenExpired())
                .findFirst()
                .orElse(null);
        
        if (token != null) {
            log.info("Access Token이 만료되어 Refresh Token으로 갱신 시도");
            return refreshAccessToken();
        }
        
        throw new RuntimeException("유효한 Cafe24 토큰이 없습니다. OAuth 인증을 다시 진행해주세요.");
    }

    @Transactional
    public String refreshAccessToken() {
        Cafe24Token token = cafe24TokenRepository.findAll().stream()
                .filter(Cafe24Token::isActive)
                .filter(t -> !t.isRefreshTokenExpired())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("유효한 Refresh Token이 없습니다. OAuth 인증을 다시 진행해주세요."));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", token.getRefreshToken());

        log.debug("Cafe24 Refresh Token Request - Form Data: {}", formData);

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        Cafe24TokenResponse tokenResponse = webClient.post()
                .uri("api/v2/oauth/token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("카페24 Access Token 갱신 API 호출 실패 - 상태 코드: {}, 응답 본문: {}", clientResponse.statusCode(), errorBody);
                                return Mono.error(new RuntimeException("카페24 Access Token 갱신 실패: " + errorBody));
                            });
                })
                .bodyToMono(Cafe24TokenResponse.class)
                .block();

        if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
            saveTokens(tokenResponse);
            log.info("Access Token 갱신 성공: {}", tokenResponse.getAccessToken());
            return tokenResponse.getAccessToken();
        } else {
            log.error("Access Token 갱신 실패: {}", tokenResponse);
            throw new RuntimeException("Failed to refresh Access Token from Cafe24.");
        }
    }
}
