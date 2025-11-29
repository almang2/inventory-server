package com.almang.inventory.global.cafe24.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.cafe24.service.Cafe24OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth/cafe24")
@RequiredArgsConstructor
@Slf4j
public class Cafe24OAuthController {

    private final Cafe24OAuthService cafe24OAuthService;

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<Map<String, String>>> cafe24OAuthCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {
        log.info("카페24 OAuth 콜백 요청 수신. Code: {}, State: {}", code, state);

        try {
            cafe24OAuthService.exchangeCodeForAccessToken(code);
            log.info("Access Token 발급 및 저장 성공.");
            
            Map<String, String> responseData = new HashMap<>();
            responseData.put("message", "Cafe24 OAuth 인증이 성공적으로 완료되었습니다.");
            
            return ResponseEntity.ok(ApiResponse.success("Cafe24 OAuth 인증 성공", responseData));
        } catch (Exception e) {
            log.error("카페24 OAuth 콜백 처리 중 오류 발생", e);
            throw e;
        }
    }

    @GetMapping("/authorize")
    public org.springframework.web.servlet.view.RedirectView redirectToCafe24Authorization() {
        String authorizationUrl = cafe24OAuthService.generateAuthorizationUrl();
        log.info("카페24 Authorization URL 생성: {}", authorizationUrl);
        return new org.springframework.web.servlet.view.RedirectView(authorizationUrl);
    }
}
