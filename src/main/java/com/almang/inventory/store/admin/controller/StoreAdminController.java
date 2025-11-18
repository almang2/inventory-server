package com.almang.inventory.store.admin.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.store.admin.dto.request.StoreAdminCreateRequest;
import com.almang.inventory.store.admin.dto.response.StoreAdminCreateResponse;
import com.almang.inventory.store.admin.service.StoreAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/store/admin")
@RequiredArgsConstructor
@Tag(name = "StoreAdmin", description = "상점 관리자 계정 관련 API")
public class StoreAdminController {

    private final StoreAdminService storeAdminService;

    @PostMapping
    @Operation(summary = "상점 관리자 계정 생성", description = "상점 관리자 계정을 생성하며 생성 시에 임시 비밀번호가 반환됩니다.")
    public ResponseEntity<ApiResponse<StoreAdminCreateResponse>> createStoreAdmin(
            @RequestBody StoreAdminCreateRequest request
    ) {
        log.info("[StoreAdminController] 관리자 생성 요청 - storeId={}, username={}", request.storeId(), request.username());
        StoreAdminCreateResponse response = storeAdminService.createStoreAdmin(request);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.SIGNUP_SUCCESS.getMessage(), response)
        );
    }
}
