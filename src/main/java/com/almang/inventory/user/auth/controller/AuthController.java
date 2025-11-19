package com.almang.inventory.user.auth.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.global.util.MaskingUtil;
import com.almang.inventory.user.auth.dto.request.ChangePasswordRequest;
import com.almang.inventory.user.auth.dto.request.LoginRequest;
import com.almang.inventory.user.auth.dto.response.AccessTokenResponse;
import com.almang.inventory.user.auth.dto.response.ChangePasswordResponse;
import com.almang.inventory.user.auth.dto.response.LoginResponse;
import com.almang.inventory.user.auth.service.AuthService;
import com.almang.inventory.user.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final TokenService tokenService;

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

    @PostMapping("/reissue")
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 확인해 액세스 토큰을 재발급합니다.")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> reissueToken(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse
    ) {
        String accessToken = tokenService.reissueAccessToken(httpServletRequest, httpServletResponse);
        AccessTokenResponse response = new AccessTokenResponse(accessToken);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.ACCESS_TOKEN_REISSUE_SUCCESS.getMessage(), response)
        );
    }

    @PostMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[AuthController] 비밀번호 변경 요청 - userId={}", userId);
        ChangePasswordResponse response = authService.changePassword(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CHANGE_PASSWORD_SUCCESS.getMessage(), response)
        );
    }
}
