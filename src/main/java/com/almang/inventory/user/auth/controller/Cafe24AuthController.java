package com.almang.inventory.user.auth.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.properties.Cafe24Properties;
import com.almang.inventory.cafe24.service.Cafe24TokenService;
import com.almang.inventory.user.auth.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cafe24/oauth")
public class Cafe24AuthController {

    private final Cafe24Properties cafe24Properties;
    private final Cafe24TokenService cafe24TokenService;
    private final RedisService redisService; // state 저장을 위한 RedisService

    /**
     * 카페24 OAuth 인증 시작 엔드포인트
     * 사용자를 카페24 인증 페이지로 리다이렉트합니다.
     * (보안 고려: 이 엔드포인트는 실제 서비스에서 관리자만 접근 가능하도록 제한해야 합니다.)
     */
    @GetMapping("/redirect")
    public RedirectView redirectToCafe24Auth() {
        // CSRF 방지를 위한 랜덤 state 값 생성
        String state = UUID.randomUUID().toString();
        redisService.saveCafe24OAuthState(state); // Redis에 저장 (10분 후 자동 만료)

        String authUrl = UriComponentsBuilder.fromUriString(cafe24Properties.getOauthUrl())
                .pathSegment("api", "v2", "oauth", "authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", cafe24Properties.getClientId())
                .queryParam("redirect_uri", cafe24Properties.getRedirectUri())
                .queryParam("scope", cafe24Properties.getScope())
                .queryParam("state", state) // Redis에 저장한 state 값 사용
                .build()
                .toUriString();

        log.info("카페24 OAuth 인증 페이지로 리다이렉트: state={}", state);
        return new RedirectView(authUrl);
    }

    /**
     * 카페24 OAuth 인증 콜백 엔드포인트
     * 카페24로부터 인증 코드를 수신하고 Access Token을 교환합니다.
     * (보안 고려: 이 엔드포인트는 Spring Security에서 permitAll()로 설정해야 합니다.)
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<String>> handleCafe24OAuthCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state, // CSRF 방지용 state 값
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        log.info("카페24 OAuth 콜백 수신: code={}, state={}, error={}, errorDescription={}", code, state, error,
                errorDescription);

        // CSRF 방지: state 값 검증 (Redis에서 확인)
        if (state == null || !redisService.hasCafe24OAuthState(state)) {
            log.error("카페24 OAuth state 값 불일치 또는 만료: receivedState={}", state);
            // state 검증 실패 시 프론트엔드로 오류 리다이렉트
            URI errorRedirectUri = UriComponentsBuilder.fromUriString(cafe24Properties.getOauthSuccessRedirectUrl())
                    .queryParam("success", "false")
                    .queryParam("message", "OAuth 인증 실패: state 값 불일치 또는 만료")
                    .build()
                    .toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(errorRedirectUri);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        // state 검증 성공 후 Redis에서 제거 (일회성 보장)
        redisService.deleteCafe24OAuthState(state);

        if (error != null) {
            log.error("카페24 OAuth 인증 실패: error={}, error_description={}", error, errorDescription);
            // OAuth 실패 시 프론트엔드로 리다이렉트하여 오류 메시지 전달
            URI errorRedirectUri = UriComponentsBuilder.fromUriString(cafe24Properties.getOauthSuccessRedirectUrl())
                    .queryParam("success", "false")
                    .queryParam("message", errorDescription != null ? errorDescription : "OAuth 인증 실패")
                    .build()
                    .toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(errorRedirectUri);
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Found (리다이렉트)
        }

        try {
            // 인증 코드를 사용하여 Access Token 교환 및 저장 (Cafe24TokenService에 위임)
            cafe24TokenService.exchangeCodeForToken(code);

            // 성공 시 프론트엔드로 리다이렉트
            URI successRedirectUri = UriComponentsBuilder.fromUriString(cafe24Properties.getOauthSuccessRedirectUrl())
                    .queryParam("success", "true")
                    .build()
                    .toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(successRedirectUri);
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Found (리다이렉트)

        } catch (Exception e) {
            log.error("Cafe24 토큰 교환 실패", e);
            URI errorRedirectUri = UriComponentsBuilder.fromUriString(cafe24Properties.getOauthSuccessRedirectUrl())
                    .queryParam("success", "false")
                    .queryParam("message", "토큰 교환 실패")
                    .build()
                    .toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(errorRedirectUri);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

    }

    @GetMapping("/success")
    public ResponseEntity<String> success(@RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "message", required = false) String message) {
        if ("true".equals(success)) {
            return ResponseEntity.ok("Cafe24 OAuth Authentication Successful!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Authentication Failed: " + (message != null ? message : "Unknown error"));
        }
    }
}
