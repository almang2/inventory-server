package com.almang.inventory.cafe24.client;

import com.almang.inventory.cafe24.service.Cafe24TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24ApiClient {

    private final Cafe24TokenService cafe24TokenService;
    private final RestClient restClient = RestClient.create();

    @Value("${cafe24.api.base-url}")
    private String baseUrl;

    @Value("${cafe24.api.client-id}")
    private String clientId;

    public String fetchOrders(String mallId, String status) {
        String accessToken = cafe24TokenService.getAccessToken(mallId);
        String startDate = java.time.LocalDate.now().minusDays(7).toString();
        String endDate = java.time.LocalDate.now().toString();

        return restClient.get()
                .uri(baseUrl + "/orders?start_date=" + startDate + "&end_date=" + endDate + "&order_status=" + status
                        + "&embed=items")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .body(String.class);
    }
}
