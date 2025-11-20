package com.almang.inventory.product.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.response.ProductResponse;
import com.almang.inventory.product.service.ProductService;
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
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
@Tag(name = "Product", description = "품목 관련 API")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "품목 등록", description = "품목을 등록하고 생성된 품목 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ProductController] 품목 등록 요청 - userId: {}", userId);
        ProductResponse response = productService.createProduct(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CREATE_PRODUCT_SUCCESS.getMessage(), response)
        );
    }

}
