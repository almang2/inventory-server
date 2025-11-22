package com.almang.inventory.store.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.store.dto.request.UpdateStoreRequest;
import com.almang.inventory.store.dto.response.UpdateStoreResponse;
import com.almang.inventory.store.service.StoreService;
import com.almang.inventory.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/order-templates")
    @Operation(summary = "상점 발주 템플릿 목록 조회", description = "현재 상점에 속한 모든 발주 템플릿을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<OrderTemplateResponse>>> getStoreOrderTemplates(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "activated", required = false) Boolean activated
    ) {
        Long userId = userPrincipal.getId();
        log.info("[StoreController] 상점 발주 템플릿 목록 조회 요청 - userId: {}, page: {}, size: {}, activated: {}",
                userId, page, size, activated);
        PageResponse<OrderTemplateResponse> response =
                storeService.getStoreOrderTemplateList(userId, page, size, activated);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_STORE_ORDER_TEMPLATE_SUCCESS.getMessage(), response)
        );
    }
}
