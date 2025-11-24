package com.almang.inventory.receipt.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import com.almang.inventory.receipt.dto.request.UpdateReceiptItemRequest;
import com.almang.inventory.receipt.dto.request.UpdateReceiptRequest;
import com.almang.inventory.receipt.dto.response.DeleteReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.DeleteReceiptResponse;
import com.almang.inventory.receipt.dto.response.ReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
import com.almang.inventory.receipt.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/receipt")
@RequiredArgsConstructor
@Tag(name = "Receipt", description = "입고 관련 API")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "발주 기반 입고 생성", description = "발주 기반 입고를 생성합니다.")
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
    @Operation(summary = "발주 기반 입고 조회", description = "발주 기반 입고를 조회합니다.")
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
    @Operation(summary = "입고 조회", description = "입고를 조회합니다.")
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

    @GetMapping
    @Operation(summary = "입고 목록 조회", description = "입고 목록을 페이지네이션, 발주처, 상태, 날짜 검색 조건과 함께 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<ReceiptResponse>>> getOrderList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "vendorId", required = false) Long vendorId,
            @RequestParam(value = "receiptStatus", required = false) ReceiptStatus status,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "endDate", required = false) LocalDate toDate
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 목록 조회 요청 - userId: {}, page: {}, size: {}, vendorId: {}, status: {}, fromDate: {}, endDate: {}",
                userId, page, size, vendorId, status, fromDate, toDate);
        PageResponse<ReceiptResponse> response =
                receiptService.getReceiptList(userId, page, size, vendorId, status, fromDate, toDate);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_RECEIPT_LIST_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/{receiptId}")
    @Operation(summary = "입고 수정", description = "입고를 수정합니다.")
    public ResponseEntity<ApiResponse<ReceiptResponse>> updateReceipt(
            @PathVariable Long receiptId,
            @Valid @RequestBody UpdateReceiptRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 수정 요청 - userId: {}, receiptId: {}", userId, receiptId);
        ReceiptResponse response = receiptService.updateReceipt(receiptId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_RECEIPT_SUCCESS.getMessage(), response)
        );
    }

    @DeleteMapping("/{receiptId}")
    @Operation(summary = "입고 삭제", description = "입고를 삭제합니다.")
    public ResponseEntity<ApiResponse<DeleteReceiptResponse>> deleteReceipt(
            @PathVariable Long receiptId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 삭제 요청 - userId: {}, receiptId: {}", userId, receiptId);
        DeleteReceiptResponse response = receiptService.deleteReceipt(receiptId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.DELETE_RECEIPT_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/receipt/{receiptItemId}")
    @Operation(summary = "입고 아이템 조회", description = "입고 아이템을 조회합니다.")
    public ResponseEntity<ApiResponse<ReceiptItemResponse>> getReceiptItem(
            @PathVariable Long receiptItemId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 아이템 조회 요청 - userId: {}, receiptItemId: {}", userId, receiptItemId);
        ReceiptItemResponse response = receiptService.getReceiptItem(receiptItemId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_RECEIPT_ITEM_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/receipt/{receiptItemId}")
    @Operation(summary = "입고 아이템 수정", description = "입고 아이템을 수정합니다.")
    public ResponseEntity<ApiResponse<ReceiptItemResponse>> updateReceiptItem(
            @PathVariable Long receiptItemId,
            @Valid @RequestBody UpdateReceiptItemRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 아이템 수정 요청 - userId: {}, receiptItemId: {}", userId, receiptItemId);
        ReceiptItemResponse response = receiptService.updateReceiptItem(receiptItemId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_RECEIPT_ITEM_SUCCESS.getMessage(), response)
        );
    }

    @DeleteMapping("/receipt/{receiptItemId}")
    @Operation(summary = "입고 아이템 삭제", description = "입고 아이템을 삭제합니다.")
    public ResponseEntity<ApiResponse<DeleteReceiptItemResponse>> deleteReceiptItem(
            @PathVariable Long receiptItemId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[ReceiptController] 입고 아이템 삭제 요청 - userId: {}, receiptItemId: {}", userId, receiptItemId);
        DeleteReceiptItemResponse response = receiptService.deleteReceiptItem(receiptItemId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.DELETE_RECEIPT_ITEM_SUCCESS.getMessage(), response)
        );
    }
}
