package com.almang.inventory.global.cafe24.controller;

import com.almang.inventory.global.cafe24.client.Cafe24ApiClient;
import com.almang.inventory.global.cafe24.dto.Cafe24OrderDetailResponse;
import com.almang.inventory.global.cafe24.dto.Cafe24OrderResponse;
import com.almang.inventory.global.cafe24.service.Cafe24OAuthService;
import com.almang.inventory.global.cafe24.service.Cafe24OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cafe24/orders")
@RequiredArgsConstructor
@Slf4j
public class Cafe24OrderSyncController {

    private final Cafe24OrderNotificationService cafe24OrderNotificationService;
    private final Cafe24OAuthService cafe24OAuthService;
    private final Cafe24ApiClient cafe24ApiClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping("/check")
    public ResponseEntity<Map<String, String>> checkNewOrders() {
        log.info("수동 Cafe24 새 주문 확인 요청");
        try {
            cafe24OrderNotificationService.checkAndNotifyNewOrders();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cafe24 새 주문 확인이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Cafe24 새 주문 확인 실패", e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cafe24 새 주문 확인 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testCafe24Api() {
        log.info("Cafe24 API 테스트 요청");
        try {
            // 유효한 Access Token 가져오기
            String accessToken = cafe24OAuthService.getValidAccessTokenOrRefresh();

            // 최근 7일간의 주문 조회
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            String startDateStr = startDate.format(DATE_FORMATTER);
            String endDateStr = endDate.format(DATE_FORMATTER);

            log.info("Cafe24 주문 조회 테스트 - 시작일: {}, 종료일: {}", startDateStr, endDateStr);

            Cafe24OrderResponse response = cafe24ApiClient.getOrders(accessToken, startDateStr, endDateStr)
                    .block();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("startDate", startDateStr);
            result.put("endDate", endDateStr);
            result.put("accessToken", accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            
            if (response != null && response.getOrders() != null) {
                // 각 주문에 대해 상세 정보 가져오기 (items 포함)
                List<Cafe24OrderResponse.Order> ordersWithItems = response.getOrders().stream()
                        .map(order -> {
                            try {
                                return enrichOrderWithItems(accessToken, order);
                            } catch (Exception e) {
                                log.warn("주문 상세 정보를 가져오는 중 오류 발생 - orderId: {}", order.getOrderId(), e);
                                return order; // 실패해도 기본 정보 반환
                            }
                        })
                        .collect(java.util.stream.Collectors.toList());
                
                result.put("orderCount", ordersWithItems.size());
                result.put("orders", ordersWithItems);
            } else {
                result.put("orderCount", 0);
                result.put("orders", null);
                result.put("response", response);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cafe24 API 테스트 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getName());
            
            // 원인 예외도 포함
            if (e.getCause() != null) {
                result.put("cause", e.getCause().getMessage());
                result.put("causeClass", e.getCause().getClass().getName());
            }
            
            return ResponseEntity.internalServerError().body(result);
        }
    }

    private Cafe24OrderResponse.Order enrichOrderWithItems(String accessToken, Cafe24OrderResponse.Order order) {
        try {
            Cafe24OrderDetailResponse detailResponse = cafe24ApiClient.getOrderDetail(accessToken, order.getOrderId())
                    .block();

            if (detailResponse != null && detailResponse.getOrder() != null 
                    && detailResponse.getOrder().getItems() != null) {
                List<Cafe24OrderResponse.OrderItem> items = detailResponse.getOrder().getItems().stream()
                        .map(detailItem -> {
                            Cafe24OrderResponse.OrderItem item = new Cafe24OrderResponse.OrderItem();
                            item.setProductCode(detailItem.getProductCode());
                            item.setProductName(detailItem.getProductName());
                            item.setQuantity(detailItem.getQuantity());
                            if (detailItem.getPrice() != null) {
                                item.setPrice(detailItem.getPrice().toString());
                            }
                            item.setOptionValue(detailItem.getOptionValue());
                            item.setOptionCode(detailItem.getOptionCode());
                            item.setVariantCode(detailItem.getVariantCode());
                            item.setItemCode(detailItem.getItemCode());
                            return item;
                        })
                        .collect(java.util.stream.Collectors.toList());
                
                order.setItems(items);
                log.debug("주문 상세 정보 가져오기 성공 - orderId: {}, items: {}개", order.getOrderId(), items.size());
            }
        } catch (Exception e) {
            log.warn("주문 상세 정보를 가져오는 중 오류 발생 - orderId: {}", order.getOrderId(), e);
        }

        return order;
    }
}

