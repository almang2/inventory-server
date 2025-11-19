package com.almang.inventory.user.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.user.dto.request.UpdateUserProfileRequest;
import com.almang.inventory.user.dto.response.UpdateUserProfileResponse;
import com.almang.inventory.user.dto.response.UserProfileResponse;
import com.almang.inventory.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/me")
    @Operation(summary = "사용자 정보 조회", description = "사용자의 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[UserController] 사용자 정보 조회 요청 - userId={}", userId);
        UserProfileResponse response = userService.getUserProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_USER_PROFILE_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/me")
    @Operation(summary = "사용자 프로필 업데이트", description = "사용자 프로필 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<UpdateUserProfileResponse>> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[UserController] 사용자 정보 수정 요청 - userId={}", userId);
        UpdateUserProfileResponse response = userService.updateUserProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_USER_PROFILE_SUCCESS.getMessage(), response)
        );
    }
}
