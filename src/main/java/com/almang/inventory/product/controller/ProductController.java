package com.almang.inventory.product.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.request.UpdateProductRequest;
import com.almang.inventory.product.dto.response.DeleteProductResponse;
import com.almang.inventory.product.dto.response.ProductResponse;
import com.almang.inventory.product.service.ProductService;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @PatchMapping("/{productId}")
    @Operation(summary = "품목 수정", description = "품목을 수정하고 수정된 품목 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ProductController] 품목 수정 요청 - userId: {}, productId: {}", userId, productId);
        ProductResponse response = productService.updateProduct(productId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_PRODUCT_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{productId}")
    @Operation(summary = "품목 상세 조회", description = "품목을 상세 조회합니다.")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ProductController] 품목 상세 조회 요청 - userId: {}, productId: {}", userId, productId);
        ProductResponse response = productService.getProductDetail(productId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_PRODUCT_DETAIL_SUCCESS.getMessage(), response)
        );
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "품목 삭제", description = "품목을 삭제합니다.")
    public ResponseEntity<ApiResponse<DeleteProductResponse>> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ProductController] 품목 삭제 요청 - userId: {}, productId: {}", userId, productId);
        DeleteProductResponse response = productService.deleteProduct(productId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.DELETE_PRODUCT_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping
    @Operation(summary = "품목 목록 조회", description = "품목 목록을 페이지네이션, 활성 여부, 이름 검색 조건과 함께 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "isActivate", required = false) Boolean isActivate,
            @RequestParam(value = "name", required = false) String nameKeyword
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ProductController] 품목 목록 조회 요청 - userId: {}, page: {}, size: {}, isActivate: {}, name: {}",
                userId, page, size, isActivate, nameKeyword);
        PageResponse<ProductResponse> response =
                productService.getProductList(userId, page, size, isActivate, nameKeyword);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_PRODUCT_LIST_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{productId}/vendor")
    @Operation(summary = "품목 발주처 조회", description = "품목 발주처를 조회합니다.")
    public ResponseEntity<ApiResponse<VendorResponse>> getVendorByProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ProductController] 품목 발주처 조회 요청 - userId: {}, productId: {}", userId, productId);
        VendorResponse response = productService.getVendorByProduct(productId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_VENDOR_BY_PRODUCT_SUCCESS.getMessage(), response)
        );
    }
}
