package com.almang.inventory.order.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.dto.request.CreateOrderRequest;
import com.almang.inventory.order.dto.request.UpdateOrderItemRequest;
import com.almang.inventory.order.dto.request.UpdateOrderRequest;
import com.almang.inventory.order.dto.response.DeleteOrderItemResponse;
import com.almang.inventory.order.dto.response.DeleteOrderResponse;
import com.almang.inventory.order.dto.response.OrderItemResponse;
import com.almang.inventory.order.dto.response.OrderResponse;
import com.almang.inventory.order.service.OrderService;
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
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Tag(name = "Order", description = "발주 관련 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "발주 생성", description = "발주를 생성하고 생성된 발주 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 생성 요청 - userId: {}", userId);
        OrderResponse response = orderService.createOrder(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CREATE_ORDER_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "발주 조회", description = "발주 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 조회 요청 - orderId: {}, userId: {}", orderId, userId);
        OrderResponse response = orderService.getOrder(orderId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_ORDER_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping
    @Operation(summary = "발주 목록 조회", description = "발주 목록을 페이지네이션, 발주처, 상태, 날짜 검색 조건과 함께 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrderList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "vendorId", required = false) Long vendorId,
            @RequestParam(value = "orderStatus", required = false) OrderStatus status,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 목록 조회 요청 - userId: {}, page: {}, size: {}, vendorId: {}, status: {}, fromDate: {}, endDate: {}",
                userId, page, size, vendorId, status, fromDate, endDate);
        PageResponse<OrderResponse> response =
                orderService.getOrderList(userId, vendorId, page, size, status, fromDate, endDate);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_ORDER_LIST_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/{orderId}")
    @Operation(summary = "발주 수정", description = "발주를 수정합니다.")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 수정 요청 - orderId: {}, userId: {}", orderId, userId);
        OrderResponse response = orderService.updateOrder(orderId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_ORDER_SUCCESS.getMessage(), response)
        );
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "발주 삭제", description = "발주를 삭제합니다.")
    public ResponseEntity<ApiResponse<DeleteOrderResponse>> deleteOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 삭제 요청 - orderId: {}, userId: {}", orderId, userId);
        DeleteOrderResponse response = orderService.deleteOrder(orderId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.DELETE_ORDER_SUCCESS.getMessage(), response)
        );
    }

    @GetMapping("/item/{orderItemId}")
    @Operation(summary = "발주 아이템 조회", description = "발주 아이템 조회합니다.")
    public ResponseEntity<ApiResponse<OrderItemResponse>> getOrderItem(
            @PathVariable Long orderItemId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 아이템 조회 요청 - orderItemId: {}, userId: {}", orderItemId, userId);
        OrderItemResponse response = orderService.getOrderItem(orderItemId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.GET_ORDER_ITEM_SUCCESS.getMessage(), response)
        );
    }

    @PatchMapping("/item/{orderItemId}")
    @Operation(summary = "발주 아이템 수정", description = "발주 아이템을 수정합니다.")
    public ResponseEntity<ApiResponse<OrderItemResponse>> updateOrderItem(
            @PathVariable Long orderItemId,
            @Valid @RequestBody UpdateOrderItemRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 아이템 수정 요청 - orderId: {}, userId: {}", orderItemId, userId);
        OrderItemResponse response = orderService.updateOrderItem(orderItemId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.UPDATE_ORDER_ITEM_SUCCESS.getMessage(), response)
        );
    }

    @DeleteMapping("/item/{orderItemId}")
    @Operation(summary = "발주 아이템 삭제", description = "발주 아이템을 삭제합니다.")
    public ResponseEntity<ApiResponse<DeleteOrderItemResponse>> deleteOrderItem(
            @PathVariable Long orderItemId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        log.info("[OrderController] 발주 아이템 삭제 요청 - orderId: {}, userId: {}", orderItemId, userId);
        DeleteOrderItemResponse response = orderService.deleteOrderItem(orderItemId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.DELETE_ORDER_ITEM_SUCCESS.getMessage(), response)
        );
    }
}
