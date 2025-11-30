package com.almang.inventory.cafe24.batch;

import com.almang.inventory.cafe24.client.Cafe24ApiClient;
import com.almang.inventory.cafe24.repository.Cafe24TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24OrderBatch {

    private final Cafe24ApiClient cafe24ApiClient;
    private final Cafe24TokenRepository cafe24TokenRepository;
    private final com.almang.inventory.cafe24.service.Cafe24OrderService cafe24OrderService;

    // Run every 10 minutes
    @Scheduled(fixedRate = 600000)
    public void fetchOrderJob() {
        log.info("Starting Cafe24 Order Batch...");

        cafe24TokenRepository.findAll().forEach(token -> {
            try {
                String mallId = token.getMallId();
                log.info("Processing orders for mall: {}", mallId);

                // 1. 입금 전 관리 (N00)
                try {
                    String responseN00 = cafe24ApiClient.fetchOrders(mallId, "N00");
                    cafe24OrderService.processBeforeDepositOrders(responseN00);
                    log.info("Processed N00 orders for mall: {}", mallId);
                } catch (Exception e) {
                    log.error("Failed to process N00 orders for mall: {}", mallId, e);
                }

                // 2. 배송 대기 관리 (N10)
                try {
                    String responseN10 = cafe24ApiClient.fetchOrders(mallId, "N10");
                    cafe24OrderService.processPreparingShipmentOrders(responseN10);
                    log.info("Processed N10 orders for mall: {}", mallId);
                } catch (Exception e) {
                    log.error("Failed to process N10 orders for mall: {}", mallId, e);
                }

            } catch (Exception e) {
                log.error("Failed to process batch for mall: {}", token.getMallId(), e);
            }
        });

        log.info("Cafe24 Order Batch finished.");
    }
}
