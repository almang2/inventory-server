package com.almang.inventory.wholesale.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.wholesale.domain.WholesaleStatus;
import com.almang.inventory.wholesale.dto.request.ConfirmWholesaleRequest;
import com.almang.inventory.wholesale.dto.request.CreatePendingWholesaleRequest;
import com.almang.inventory.wholesale.dto.request.UpdateWholesaleRequest;
import com.almang.inventory.wholesale.dto.response.CancelWholesaleResponse;
import com.almang.inventory.wholesale.dto.response.ConfirmWholesaleResponse;
import com.almang.inventory.wholesale.dto.response.WholesaleResponse;
import com.almang.inventory.wholesale.service.WholesaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/wholesales")
@RequiredArgsConstructor
@Tag(name = "Wholesale", description = "도매 출고 관련 API")
public class WholesaleController {

    private final WholesaleService wholesaleService;

    @PostMapping("/pending")
    @Operation(summary = "출고 대기 생성", description = "주문서 기준으로 출고 대기 상태를 생성합니다. 창고 재고가 주문 수량 이상이어야 합니다.")
    public ResponseEntity<ApiResponse<WholesaleResponse>> createPendingWholesale(
            @Valid @RequestBody CreatePendingWholesaleRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[WholesaleController] 출고 대기 생성 요청 - userId: {}", userId);
        WholesaleResponse response = wholesaleService.createPendingWholesale(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success("출고 대기 생성에 성공했습니다.", response)
        );
    }

    @GetMapping("/{wholesaleId}")
    @Operation(summary = "출고 조회", description = "출고를 조회합니다.")
    public ResponseEntity<ApiResponse<WholesaleResponse>> getWholesale(
            @PathVariable Long wholesaleId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[WholesaleController] 출고 조회 요청 - userId: {}, wholesaleId: {}", userId, wholesaleId);
        WholesaleResponse response = wholesaleService.getWholesale(wholesaleId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("출고 조회에 성공했습니다.", response)
        );
    }

    @GetMapping
    @Operation(summary = "출고 목록 조회", description = "출고 목록을 페이지네이션, 상태, 날짜, 주문서 참조 번호 검색 조건과 함께 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<WholesaleResponse>>> getWholesaleList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "status", required = false) WholesaleStatus status,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "orderReference", required = false) String orderReference
    ) {
        Long userId = userPrincipal.getId();
        log.info("[WholesaleController] 출고 목록 조회 요청 - userId: {}, page: {}, size: {}, status: {}, fromDate: {}, toDate: {}, orderReference: {}",
                userId, page, size, status, fromDate, toDate, orderReference);
        PageResponse<WholesaleResponse> response =
                wholesaleService.getWholesaleList(userId, page, size, status, fromDate, toDate, orderReference);

        return ResponseEntity.ok(
                ApiResponse.success("출고 목록 조회에 성공했습니다.", response)
        );
    }

    @PatchMapping("/{wholesaleId}/confirm")
    @Operation(summary = "출고 완료 처리", description = "관리자가 피킹/패킹 후 출고를 완료 처리합니다. 재고가 자동으로 차감됩니다.")
    public ResponseEntity<ApiResponse<ConfirmWholesaleResponse>> confirmWholesale(
            @PathVariable Long wholesaleId,
            @RequestBody(required = false) ConfirmWholesaleRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[WholesaleController] 출고 완료 처리 요청 - userId: {}, wholesaleId: {}", userId, wholesaleId);
        
        ConfirmWholesaleRequest confirmRequest = request != null ? request : new ConfirmWholesaleRequest(null);
        ConfirmWholesaleResponse response = wholesaleService.confirmWholesale(wholesaleId, confirmRequest, userId);

        return ResponseEntity.ok(
                ApiResponse.success("출고 완료 처리에 성공했습니다.", response)
        );
    }

    @PatchMapping("/{wholesaleId}")
    @Operation(summary = "출고 수정", description = "출고 대기 상태의 출고 정보와 항목들을 수정합니다.")
    public ResponseEntity<ApiResponse<WholesaleResponse>> updateWholesale(
            @PathVariable Long wholesaleId,
            @Valid @RequestBody UpdateWholesaleRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[WholesaleController] 출고 수정 요청 - userId: {}, wholesaleId: {}", userId, wholesaleId);
        WholesaleResponse response = wholesaleService.updateWholesale(wholesaleId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success("출고 수정에 성공했습니다.", response)
        );
    }

    @PatchMapping("/{wholesaleId}/cancel")
    @Operation(summary = "출고 취소", description = "출고 대기 상태의 출고를 취소합니다. 출고 예정 수량이 해제됩니다.")
    public ResponseEntity<ApiResponse<CancelWholesaleResponse>> cancelWholesale(
            @PathVariable Long wholesaleId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[WholesaleController] 출고 취소 요청 - userId: {}, wholesaleId: {}", userId, wholesaleId);
        CancelWholesaleResponse response = wholesaleService.cancelWholesale(wholesaleId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("출고 취소에 성공했습니다.", response)
        );
    }
}

