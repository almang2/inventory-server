package com.almang.inventory.global.cafe24.scheduler;

import com.almang.inventory.global.cafe24.service.Cafe24OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24OrderSyncScheduler {

    private final Cafe24OrderNotificationService cafe24OrderNotificationService;

    /**
     * 매 10분마다 Cafe24 새 주문을 확인하고 알림을 전송합니다.
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void checkNewOrders() {
        log.info("스케줄러: Cafe24 새 주문 확인 시작");
        try {
            cafe24OrderNotificationService.checkAndNotifyNewOrders();
            log.info("스케줄러: Cafe24 새 주문 확인 완료");
        } catch (Exception e) {
            log.error("스케줄러: Cafe24 새 주문 확인 실패", e);
        }
    }
}

