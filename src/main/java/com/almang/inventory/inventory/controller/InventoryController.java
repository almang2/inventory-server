package com.almang.inventory.inventory.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.inventory.dto.request.UpdateInventoryRequest;
import com.almang.inventory.inventory.dto.response.InventoryResponse;
import com.almang.inventory.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "재고 관련 API")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "재고 목록 조회", description = "상점의 재고 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getStoreInventoryList(
            @RequestParam(name = "page", required = false) int page,
            @RequestParam(name = "size", required = false) int size,
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "sort", required = false) String sort,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[InventoryController] 상점 재고 전체 조회 요청 - userId: {}, page: {}, size: {}, scope: {}, q: {}, sort: {}",
                userId, page, size, scope, q, sort);
        PageResponse<InventoryResponse> response =
                inventoryService.getStoreInventoryList(userId, page, size, scope, q, sort);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_STORE_INVENTORY_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/{inventoryId}")
    @Operation(summary = "재고 수동 수정", description = "재고를 수정하고 수정된 재고 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable Long inventoryId,
            @Valid @RequestBody UpdateInventoryRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[InventoryController] 재고 수동 수정 요청 - userId: {}", userId);
        InventoryResponse response = inventoryService.updateInventory(inventoryId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_INVENTORY_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{inventoryId}")
    @Operation(summary = "재고 아이디 기반 재고 조회", description = "재고 아이디를 통해 재고를 조회하고 재고 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable Long inventoryId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[InventoryController] 재고 아이디 기반 재고 조회 요청 - userId: {}, inventoryId: {}", userId, inventoryId);
        InventoryResponse response = inventoryService.getInventory(inventoryId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_INVENTORY_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "품목 아이디 기반 재고 조회", description = "품목 아이디를 통해 재고를 조회하고 재고 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[InventoryController] 품목 기준 재고 조회 요청 - userId: {}, productId: {}", userId, productId);
        InventoryResponse response = inventoryService.getInventoryByProduct(productId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_INVENTORY_BY_PRODUCT_SUCCESS.getMessage(), response)
        );
    }
}
