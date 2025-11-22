package com.almang.inventory.vendor.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.vendor.dto.request.CreateOrderTemplateRequest;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.request.UpdateVendorRequest;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
@Tag(name = "Vendor", description = "발주처 관련 API")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @Operation(summary = "발주처 등록", description = "발주처를 등록하고 생성된 발주처 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<VendorResponse>> createVendor(
            @Valid @RequestBody CreateVendorRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[VendorController] 발주처 등록 요청 - userId: {}", userId);
        VendorResponse response = vendorService.createVendor(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CREATE_VENDOR_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/{vendorId}")
    @Operation(summary = "발주처 수정", description = "발주처를 수정하고 수정된 발주처 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<VendorResponse>> updateVendor(
            @PathVariable Long vendorId,
            @Valid @RequestBody UpdateVendorRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[VendorController] 발주처 수정 요청 - vendorId: {}, userId: {}", vendorId, userId);
        VendorResponse response = vendorService.updateVendor(vendorId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_VENDOR_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{vendorId}")
    @Operation(summary = "발주처 상세 조회", description = "발주처 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<VendorResponse>> getVendorDetail(
            @PathVariable Long vendorId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[VendorController] 발주처 상세 조회 요청 - vendorId: {}, userId: {}", vendorId, userId);
        VendorResponse response = vendorService.getVendorDetail(vendorId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_VENDOR_DETAIL_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping
    @Operation(summary = "발주처 목록 조회", description = "발주처 목록을 페이지네이션, 활성 여부, 이름 검색 조건과 함께 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<VendorResponse>>> getVendorList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "isActivate", required = false) Boolean isActivate,
            @RequestParam(value = "name", required = false) String nameKeyword
    ) {
        Long userId = userPrincipal.getId();
        log.info("[VendorController] 발주처 목록 조회 요청 - userId: {}, page: {}, size: {}, isActivate: {}, name: {}",
                userId, page, size, isActivate, nameKeyword);
        PageResponse<VendorResponse> response =
                vendorService.getVendorList(userId, page, size, isActivate, nameKeyword);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_VENDOR_LIST_SUCCESS.getMessage(), response)
        );
    }

    @PostMapping("/{vendorId}/order-template")
    @Operation(summary = "발주 템플릿 등록", description = "발주 템플릿을 등록하고 생성된 발주 템플릿을 반환합니다.")
    public ResponseEntity<ApiResponse<OrderTemplateResponse>> createOrderTemplate(
            @PathVariable Long vendorId,
            @Valid @RequestBody CreateOrderTemplateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[VendorController] 발주 템플릿 생성 요청 - userId: {}, vendorId: {}", userId, vendorId);
        OrderTemplateResponse response = vendorService.createOrderTemplate(vendorId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CREATE_ORDER_TEMPLATE_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{vendorId}/order-templates")
    @Operation(summary = "발주처 발주 템플릿 조회", description = "발주처의 발주 템플릿을 조회합니다.")
    public ResponseEntity<ApiResponse<List<OrderTemplateResponse>>> getOrderTemplates(
            @PathVariable Long vendorId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "activated", required = false) Boolean activated
    ) {
        Long userId = userPrincipal.getId();
        log.info("[VendorController] 발주처 발주 템플릿 조회 요청 - userId: {}, vendorId: {}", userId, vendorId);
        List<OrderTemplateResponse> response = vendorService.getOrderTemplates(vendorId, userId, activated);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_VENDOR_ORDER_TEMPLATE_SUCCESS.getMessage(), response)
        );
    }
}
