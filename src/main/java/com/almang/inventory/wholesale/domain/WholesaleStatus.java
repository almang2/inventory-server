package com.almang.inventory.wholesale.domain;

public enum WholesaleStatus {
    PENDING,      // 출고 대기 (피킹/패킹 전)
    CONFIRMED,    // 출고 완료 (재고 차감 완료)
    CANCELED      // 출고 취소
}

