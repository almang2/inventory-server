package com.almang.inventory.store.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.store.dto.request.UpdateStoreRequest;
import com.almang.inventory.store.dto.response.UpdateStoreResponse;
import com.almang.inventory.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
@Tag(name = "Store", description = "상점 관련 API")
public class StoreController {

    private final StoreService storeService;

    @PatchMapping
    @Operation(summary = "상점 정보 업데이트", description = "상점 정보를 업데이트 하고 업데이트한 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<UpdateStoreResponse>> updateStore(
            @Valid @RequestBody UpdateStoreRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[StoreController] 상점 정보 업데이트 요청 - userId: {}", userId);
        UpdateStoreResponse response = storeService.updateStore(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_STORE_SUCCESS.getMessage(), response)
        );
    }
}
