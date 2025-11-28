package com.almang.inventory.global.cafe24.controller;

import com.almang.inventory.global.cafe24.service.Cafe24OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/oauth/cafe24")
@RequiredArgsConstructor
@Slf4j
public class Cafe24OAuthController {

    private final Cafe24OAuthService cafe24OAuthService;

    @GetMapping("/callback")
    public RedirectView cafe24OAuthCallback(@RequestParam("code") String code,
                                            @RequestParam(value = "state", required = false) String state) {
        log.info("카페24 OAuth 콜백 요청 수신. Code: {}, State: {}", code, state);

        String accessToken = cafe24OAuthService.exchangeCodeForAccessToken(code);

        // TODO: Access Token을 안전하게 저장하는 로직 구현 (DB, Redis 등)
        // cafe24OAuthService.saveAccessToken(accessToken);

        // 프론트엔드의 특정 페이지로 리다이렉션 (성공 메시지 등 전달)
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("https://api.almang2.com/swagger-ui/index.html");
        return redirectView;
    }

    @GetMapping("/authorize")
    public RedirectView redirectToCafe24Authorization() {
        String authorizationUrl = cafe24OAuthService.generateAuthorizationUrl();
        log.info("카페24 Authorization URL 생성: {}", authorizationUrl);
        return new RedirectView(authorizationUrl);
    }
}
