package com.almang.inventory.order.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
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
    private LocalDate quoteReceivedAt;

    // 입금 확인일
    @Column(name = "deposit_confirmed_at")
    private LocalDate depositConfirmedAt;

    // 활성 여부
    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    // 총액 (단위: 원, 필요시 BigDecimal로 변경)
    @Column(name = "total_price")
    private Integer totalPrice;

    // 발주 상세 목록
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void validateVendorNotChanged(Long requestVendorId) {
        if (requestVendorId == null) {
            return;
        }
        if (!this.vendor.getId().equals(requestVendorId)) {
            throw new BaseException(ErrorCode.VENDOR_CHANGE_NOT_ALLOWED);
        }
    }

    public void updateStatus(OrderStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void cancel() {
        if (this.status == OrderStatus.DELIVERED) {
            throw new BaseException(ErrorCode.ORDER_ALREADY_DELIVERED);
        }
        this.status = OrderStatus.CANCELED;
        this.activated = false;
    }

    public void updateMessageAndActivated(String orderMessage, Boolean activated) {
        if (orderMessage != null) {
            this.orderMessage = orderMessage;
        }
        if (activated != null) {
            this.activated = activated;
        }
    }

    public void updateSchedule(Integer leadTime, LocalDate quoteReceivedAt, LocalDate depositConfirmedAt) {
        if (leadTime != null) {
            this.leadTime = leadTime;
            this.expectedArrival = LocalDate.now().plusDays(leadTime);
        }

        if (quoteReceivedAt != null) {
            this.quoteReceivedAt = quoteReceivedAt;
        }

        if (depositConfirmedAt != null) {
            this.depositConfirmedAt = depositConfirmedAt;
        }
    }

    public void updateTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }
}
