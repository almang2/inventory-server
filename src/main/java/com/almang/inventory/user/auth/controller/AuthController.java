package com.almang.inventory.user.auth.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.util.MaskingUtil;
import com.almang.inventory.user.auth.dto.request.LoginRequest;
import com.almang.inventory.user.auth.dto.response.LoginResponse;
import com.almang.inventory.user.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자의 로그인 요청을 처리합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpServletResponse
    ) {
        log.info("[AuthController] 로그인 요청 - username={}", MaskingUtil.maskUsername(request.username()));
        LoginResponse response = authService.login(request, httpServletResponse);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.LOGIN_SUCCESS.getMessage(), response)
        );
    }
}
