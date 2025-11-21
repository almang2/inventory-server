package com.almang.inventory.vendor.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
