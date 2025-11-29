package com.almang.inventory.global.cafe24.service;

import com.almang.inventory.global.cafe24.client.Cafe24ApiClient;
import com.almang.inventory.global.cafe24.dto.Cafe24OrderDetailResponse;
import com.almang.inventory.global.cafe24.dto.Cafe24OrderResponse;
import com.almang.inventory.wholesale.service.WholesaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Cafe24OrderNotificationService {

    private final Cafe24OAuthService cafe24OAuthService;
    private final Cafe24ApiClient cafe24ApiClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final WholesaleService wholesaleService;

    @Value("${monitoring.discord.error-webhook-url:}")
    private String discordWebhookUrl;

    @Value("${monitoring.discord.enabled:false}")
    private boolean discordEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String REDIS_KEY_PREFIX = "cafe24:notified:order:";
    private static final int NOTIFICATION_EXPIRY_DAYS = 7; // 7일간 중복 알림 방지

    public void checkAndNotifyNewOrders() {
        log.info("Cafe24 새 주문 확인 시작");

        try {
            // 유효한 Access Token 가져오기 (만료 시 자동 갱신)
            String accessToken = cafe24OAuthService.getValidAccessTokenOrRefresh();

            // 최근 1일간의 주문 조회
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(1);
            String startDateStr = startDate.format(DATE_FORMATTER);
            String endDateStr = endDate.format(DATE_FORMATTER);

            log.info("Cafe24 주문 조회 - 시작일: {}, 종료일: {}", startDateStr, endDateStr);

            Cafe24OrderResponse response = cafe24ApiClient.getOrders(accessToken, startDateStr, endDateStr)
                    .block();

            if (response == null || response.getOrders() == null || response.getOrders().isEmpty()) {
                log.info("새로운 Cafe24 주문이 없습니다.");
                return;
            }

            log.info("Cafe24 주문 {}건 발견", response.getOrders().size());

            // 새로운 주문만 필터링 (이미 알림을 보낸 주문 제외)
            List<Cafe24OrderResponse.Order> newOrders = response.getOrders().stream()
                    .filter(order -> !isAlreadyNotified(order.getOrderId()))
                    .collect(Collectors.toList());

            if (newOrders.isEmpty()) {
                log.info("모든 주문에 대해 이미 알림을 보냈습니다.");
                return;
            }

            log.info("새로운 주문 {}건 발견", newOrders.size());

            // 각 주문에 대해 상세 정보를 가져와서 알림 전송 및 도매 주문 생성
            for (Cafe24OrderResponse.Order order : newOrders) {
                try {
                    // 주문 상세 정보 가져오기 (items 포함)
                    Cafe24OrderResponse.Order orderWithItems = enrichOrderWithItems(accessToken, order);
                    
                    // 도매 주문 생성 및 재고 차감
                    try {
                        wholesaleService.createWholesaleFromCafe24Order(orderWithItems);
                        log.info("도매 주문 생성 완료 - orderId: {}", orderWithItems.getOrderId());
                    } catch (Exception e) {
                        log.error("도매 주문 생성 실패 - orderId: {}", orderWithItems.getOrderId(), e);
                        // 도매 주문 생성 실패해도 알림은 전송
                    }
                    
                    sendOrderNotification(orderWithItems);
                    markAsNotified(order.getOrderId());
                } catch (Exception e) {
                    log.warn("주문 상세 정보를 가져오는 중 오류 발생 - orderId: {}, 기본 정보로 알림 전송", order.getOrderId(), e);
                    // 상세 정보를 가져오지 못해도 기본 정보로 알림 전송
                    sendOrderNotification(order);
                    markAsNotified(order.getOrderId());
                }
            }

            log.info("Cafe24 주문 알림 완료 - {}건", newOrders.size());

        } catch (Exception e) {
            log.error("Cafe24 주문 확인 중 오류 발생", e);
            throw e;
        }
    }

    @Async
    private void sendOrderNotification(Cafe24OrderResponse.Order order) {
        if (!discordEnabled || discordWebhookUrl == null || discordWebhookUrl.isBlank()) {
            log.warn("Discord 알림이 비활성화되어 있습니다. 주문 ID: {}", order.getOrderId());
            return;
        }

        try {
            String message = buildOrderNotificationMessage(order);
            sendToDiscord(message);
            log.info("Cafe24 주문 알림 전송 완료 - orderId: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Cafe24 주문 알림 전송 실패 - orderId: {}", order.getOrderId(), e);
        }
    }

    private String buildOrderNotificationMessage(Cafe24OrderResponse.Order order) {
        StringBuilder message = new StringBuilder();
        message.append("🛒 **새로운 Cafe24 주문이 들어왔습니다!**\n\n");
        
        // 주문 기본 정보
        message.append("📦 주문번호: `").append(order.getOrderId()).append("`\n");
        message.append("📅 주문일시: `").append(order.getOrderDate()).append("`\n");
        
        if (order.getPaid() != null) {
            message.append("💳 결제여부: `").append("T".equals(order.getPaid()) ? "결제완료" : "미결제").append("`\n");
        }
        if (order.getCanceled() != null && "T".equals(order.getCanceled())) {
            message.append("❌ 취소됨\n");
        }
        
        // 주문 금액 정보
        if (order.getInitialOrderAmount() != null) {
            Cafe24OrderResponse.OrderAmount amount = order.getInitialOrderAmount();
            if (amount.getOrderPriceAmount() != null) {
                message.append("💰 주문금액: `").append(amount.getOrderPriceAmount()).append("원`\n");
            }
            if (amount.getShippingFee() != null && !"0.00".equals(amount.getShippingFee())) {
                message.append("🚚 배송비: `").append(amount.getShippingFee()).append("원`\n");
            }
            if (amount.getTotalAmountDue() != null) {
                message.append("💵 총 금액: `").append(amount.getTotalAmountDue()).append("원`\n");
            }
        }
        
        if (order.getPaymentMethodName() != null && !order.getPaymentMethodName().isEmpty()) {
            message.append("💳 결제수단: `").append(String.join(", ", order.getPaymentMethodName())).append("`\n");
        }
        message.append("\n");

        // 주문자 정보
        if (order.getBillingName() != null) {
            message.append("👤 주문자: `").append(order.getBillingName()).append("`\n");
        }
        if (order.getMemberEmail() != null) {
            message.append("📧 이메일: `").append(order.getMemberEmail()).append("`\n");
        }
        if (order.getMemberId() != null) {
            message.append("🆔 회원ID: `").append(order.getMemberId()).append("`\n");
        }
        message.append("\n");

        // 주문 상품 정보 (items가 없을 수 있음)
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            message.append("🛍️ 주문 상품:\n");
            for (Cafe24OrderResponse.OrderItem item : order.getItems()) {
                if (item.getProductName() != null) {
                    message.append("  - **").append(item.getProductName()).append("**");
                } else if (item.getProductCode() != null) {
                    message.append("  - 상품코드: `").append(item.getProductCode()).append("`");
                }
                if (item.getQuantity() != null) {
                    message.append(" × ").append(item.getQuantity()).append("개");
                }
                if (item.getPrice() != null) {
                    message.append(" (").append(item.getPrice()).append("원)");
                }
                if (item.getOptionValue() != null) {
                    message.append(" [옵션: ").append(item.getOptionValue()).append("]");
                }
                message.append("\n");
            }
        } else {
            message.append("ℹ️ 주문 상품 정보는 주문 상세 API에서 확인 가능합니다.\n");
        }

        return message.toString();
    }

    private void sendToDiscord(String content) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("content", content);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(discordWebhookUrl, entity, String.class);
        } catch (Exception e) {
            log.warn("Discord로 주문 알림 전송에 실패했습니다: {}", e.getMessage());
        }
    }

    private boolean isAlreadyNotified(String orderId) {
        String key = REDIS_KEY_PREFIX + orderId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void markAsNotified(String orderId) {
        String key = REDIS_KEY_PREFIX + orderId;
        // 7일간 저장 (중복 알림 방지)
        Duration ttl = Duration.ofDays(NOTIFICATION_EXPIRY_DAYS);
        redisTemplate.opsForValue().set(key, "notified", ttl);
    }

    private Cafe24OrderResponse.Order enrichOrderWithItems(String accessToken, Cafe24OrderResponse.Order order) {
        try {
            Cafe24OrderDetailResponse detailResponse = cafe24ApiClient.getOrderDetail(accessToken, order.getOrderId())
                    .block();

            if (detailResponse != null && detailResponse.getOrder() != null 
                    && detailResponse.getOrder().getItems() != null) {
                // 상세 정보에서 items를 가져와서 변환
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

