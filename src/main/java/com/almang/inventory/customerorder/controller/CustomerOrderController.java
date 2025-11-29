package com.almang.inventory.customerorder.controller;

import com.almang.inventory.customerorder.dto.request.CustomerOrderRequest;
import com.almang.inventory.customerorder.service.CustomerOrderService;
import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer-orders") // 카페24 연동을 위한 기본 경로
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    /**
     * 카페24로부터 주문 정보를 수신하고 처리하는 API 엔드포인트
     * (재고 감소 로직은 CustomerOrderService에서 정책에 따라 구현 대기 중)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 성공적으로 생성되었음을 의미
    public ApiResponse<Long> receiveCafe24Order(@Valid @RequestBody CustomerOrderRequest request) {
        log.info("카페24로부터 주문 정보 수신: Order ID = {}", request.getCafe24OrderId());

        Long customerOrderId = customerOrderService.createCustomerOrderAndProcessStock(request);

        return ApiResponse.success(SuccessMessage.CUSTOMER_ORDER_CREATED, customerOrderId);
    }

    // 추가적인 고객 주문 관련 API 엔드포인트가 필요할 수 있습니다.
    // 예: 특정 기간 동안의 고객 주문 조회, 특정 고객의 주문 내역 조회 등
}
