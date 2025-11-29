package com.almang.inventory.global.cafe24.client;

import com.almang.inventory.global.cafe24.dto.Cafe24OrderDetailResponse;
import com.almang.inventory.global.cafe24.dto.Cafe24OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class Cafe24ApiClient {

    private final WebClient webClient;

    public Cafe24ApiClient(WebClient.Builder webClientBuilder,
                          @Value("${cafe24.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<Cafe24OrderResponse> getOrders(String accessToken, String startDate, String endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders")
                        .queryParam("start_date", startDate)
                        .queryParam("end_date", endDate)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("X-Cafe24-Api-Version", "2025-09-01")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                String errorMsg = String.format("Cafe24 API 호출 실패 - 상태 코드: %s, 응답 본문: %s", 
                                        clientResponse.statusCode(), errorBody);
                                return Mono.error(new RuntimeException(errorMsg));
                            });
                })
                .bodyToMono(String.class)
                .flatMap(rawResponse -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return Mono.just(mapper.readValue(rawResponse, Cafe24OrderResponse.class));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e));
                    }
                });
    }

    public Mono<Cafe24OrderDetailResponse> getOrderDetail(String accessToken, String orderId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/{orderId}")
                        .queryParam("embed", "items")
                        .build(orderId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("X-Cafe24-Api-Version", "2025-09-01")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                String errorMsg = String.format("Cafe24 주문 상세 API 호출 실패 - 상태 코드: %s, 응답 본문: %s", 
                                        clientResponse.statusCode(), errorBody);
                                return Mono.error(new RuntimeException(errorMsg));
                            });
                })
                .bodyToMono(String.class)
                .flatMap(rawResponse -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return Mono.just(mapper.readValue(rawResponse, Cafe24OrderDetailResponse.class));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e));
                    }
                });
    }
}
