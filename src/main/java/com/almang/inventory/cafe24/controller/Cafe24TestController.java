package com.almang.inventory.cafe24.controller;

import com.almang.inventory.cafe24.client.Cafe24ApiClient;
import com.almang.inventory.cafe24.repository.Cafe24TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cafe24")
@RequiredArgsConstructor
public class Cafe24TestController {

    private final Cafe24ApiClient cafe24ApiClient;
    private final Cafe24TokenRepository cafe24TokenRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final com.almang.inventory.cafe24.service.Cafe24OrderService cafe24OrderService;

    @GetMapping("/customerorders/test")
    public ResponseEntity<?> getOrders() {
        var tokens = cafe24TokenRepository.findAll();
        if (tokens.isEmpty()) {
            return ResponseEntity.status(401).body(java.util.Map.of(
                    "success", false,
                    "error", "유효한 Cafe24 토큰이 없습니다. OAuth 인증을 다시 진행해주세요."));
        }

        String mallId = tokens.get(0).getMallId();

        try {
            String jsonResponse = cafe24ApiClient.fetchOrders(mallId, "N00");

            // DB에 저장 (입금 전 주문 처리 로직 사용)
            java.util.List<com.almang.inventory.customerorder.domain.CustomerOrder> savedOrders = cafe24OrderService
                    .processBeforeDepositOrders(jsonResponse);

            // DTO 변환
            java.util.List<com.almang.inventory.customerorder.dto.CustomerOrderResponseDto> responseDtos = savedOrders
                    .stream()
                    .map(com.almang.inventory.customerorder.dto.CustomerOrderResponseDto::from)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(responseDtos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "success", false,
                    "error", "주문 조회 및 저장 실패: " + e.getMessage()));
        }
    }
}
