package com.almang.inventory.receipt.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
import com.almang.inventory.receipt.service.ReceiptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/receipt")
@RequiredArgsConstructor
@Tag(name = "Receipt", description = "입고 관련 API")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/from-order/{orderId}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> createReceiptFromOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 발주 기반 입고 생성 요청 - userId: {}, orderId: {}", userId, orderId);
        ReceiptResponse response = receiptService.createReceiptFromOrder(orderId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CREATE_RECEIPT_FROM_ORDER_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/from-order/{orderId}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptFromOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 발주 기반 입고 조회 요청 - userId: {}, orderId: {}", userId, orderId);
        ReceiptResponse response = receiptService.getReceiptFromOrder(orderId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_RECEIPT_FROM_ORDER_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceipt(
            @PathVariable Long receiptId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 조회 요청 - userId: {}, receiptId: {}", userId, receiptId);
        ReceiptResponse response = receiptService.getReceipt(receiptId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_RECEIPT_SUCCESS.getMessage(), response)
        );
    }

}
