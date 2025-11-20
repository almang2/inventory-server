package com.almang.inventory.store.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
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
     * 기본 알림 임계치 (0.00 ~ 1.00)
     * 예: 0.20 → 20% 이하로 떨어지면 알림
     */
    @Column(name = "default_count_check_threshold", precision = 3, scale = 2, nullable = false)
    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private BigDecimal defaultCountCheckThreshold;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateActivation(boolean isActivate) {
        this.isActivate = isActivate;
    }

    public void updateThreshold(BigDecimal defaultCountCheckThreshold) {
        this.defaultCountCheckThreshold = defaultCountCheckThreshold;
    }
}
