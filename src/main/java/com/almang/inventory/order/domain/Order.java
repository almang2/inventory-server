package com.almang.inventory.order.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.vendor.domain.Vendor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    // 상점
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // 발주처
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private OrderStatus status;

    // 발주 메시지 (템플릿 + 추가 문구)
    @Column(name = "order_message", nullable = false, columnDefinition = "TEXT")
    private String orderMessage;

    // 리드타임(일)
    @Column(name = "lead_time")
    private Integer leadTime;

    // 예상 도착일
    @Column(name = "expected_arrival")
    private LocalDate expectedArrival;

    // 견적 수신일
    @Column(name = "quote_received_at")
    private LocalDateTime quoteReceivedAt;

    // 입금 확인일
    @Column(name = "deposit_confirmed_at")
    private LocalDateTime depositConfirmedAt;

    // 활성 여부
    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    // 원가 / 판매가 / 도매가 (단위: 원, 필요시 BigDecimal로 변경)
    @Column(name = "cost_price")
    private Integer costPrice;

    @Column(name = "retail_price")
    private Integer retailPrice;

    @Column(name = "wholesale_price")
    private Integer wholesalePrice;

    // 발주 상세 목록
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
