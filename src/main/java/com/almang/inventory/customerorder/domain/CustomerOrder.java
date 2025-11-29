package com.almang.inventory.customerorder.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.user.domain.User; // 고객 정보가 필요하다면 나중에 User 엔티티와 연결할 수 있습니다.
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CustomerOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_order_id")
    private Long id;

    // 카페24 주문 고유 번호
    @Column(name = "cafe24_order_id", unique = true, nullable = false, length = 50)
    private String cafe24OrderId;

    // 주문 일시
    @Column(name = "order_at", nullable = false)
    private LocalDateTime orderAt;

    // 결제 완료 여부
    @Column(name = "is_paid", nullable = false)
    private boolean isPaid;

    // 취소 여부
    @Column(name = "is_canceled", nullable = false)
    private boolean isCanceled;

    // 결제 수단 명
    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    // 실제 결제 금액
    @Column(name = "payment_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal paymentAmount;

    // 주문자 이름 (Billing Name)
    @Column(name = "billing_name", nullable = false, length = 100)
    private String billingName;

    // 회원 ID (Cafe24 회원 ID)
    @Column(name = "member_id", length = 100)
    private String memberId;

    // 회원 이메일
    @Column(name = "member_email", length = 100)
    private String memberEmail;

    // 초기 주문 금액 (상품 가격 합계)
    @Column(name = "initial_order_price_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal initialOrderPriceAmount;

    // 배송비
    @Column(name = "shipping_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal shippingFee;

    // 고객 주문 상품 상세 목록
    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerOrderItem> items = new ArrayList<>();

    // 편의 메서드: 주문 항목 추가
    public void addOrderItem(CustomerOrderItem item) {
        items.add(item);
        item.setCustomerOrder(this);
    }

    // 결제 상태 업데이트
    public void updatePaidStatus(boolean isPaid) {
        this.isPaid = isPaid;
    }

    // 취소 상태 업데이트
    public void updateCanceledStatus(boolean isCanceled) {
        this.isCanceled = isCanceled;
    }
}
