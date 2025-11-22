package com.almang.inventory.order.template.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.order.template.dto.request.UpdateOrderTemplateRequest;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.order.template.service.OrderTemplateService;
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
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/order-template")
@RequiredArgsConstructor
@Tag(name = "OrderTemplate", description = "발주 템플릿 관련 API")
public class OrderTemplateController {

    private final OrderTemplateService orderTemplateService;

    @PatchMapping("/{orderTemplateId}")
    @Operation(summary = "발주 템플릿 수정", description = "발주 템플릿을 수정하고 수정된 템플릿 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<OrderTemplateResponse>> updateOrderTemplate(
            @PathVariable Long orderTemplateId,
            @Valid @RequestBody UpdateOrderTemplateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderTemplateController] 발주 템플릿 수정 요청 - userId: {}, orderTemplateId: {}", userId, orderTemplateId);
        OrderTemplateResponse response = orderTemplateService.updateOrderTemplate(orderTemplateId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_ORDER_TEMPLATE_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{orderTemplateId}")
    @Operation(summary = "발주 템플릿 상세 조회", description = "발주 템플릿을 상세 조회합니다.")
    public ResponseEntity<ApiResponse<OrderTemplateResponse>> getOrderTemplateDetail(
            @PathVariable Long orderTemplateId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderTemplateController] 발주 템플릿 상세 조회 요청 - userId: {}, orderTemplateId: {}", userId, orderTemplateId);
        OrderTemplateResponse response = orderTemplateService.getOrderTemplateDetail(orderTemplateId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_ORDER_TEMPLATE_DETAIL.getMessage(), response)
        );
    }
}
