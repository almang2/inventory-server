package com.almang.inventory.global.cafe24.client;

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
                .header("X-Cafe24-Api-Version", "2024-03-01")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Cafe24OrderResponse.class);
    }
}
