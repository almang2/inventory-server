package com.almang.inventory.store.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "is_activate", nullable = false)
    private boolean isActivate;

    /**
     * 기본 알림 임계치 (0.00 ~ 0.99)
     * 예: 0.20 → 20% 이하로 떨어지면 알림
     */
    @Column(name = "default_count_check_threshold", nullable = false)
    private Double defaultCountCheckThreshold;
}
