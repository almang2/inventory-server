package com.almang.inventory.customerorder.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CustomerOrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_order_item_id")
    private Long id;

    // 어떤 고객 주문에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    // 어떤 상품인지 (기존 Product 엔티티와 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 카페24에서 넘어오는 상품 코드 (Product 엔티티의 code와 매핑)
    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    // 상품명 (주문 시점의 상품명)
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    // 주문 수량
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // 상품 옵션 값 (예: 종류=반짝반짝 스피아민트 미백)
    @Column(name = "option_value", length = 255)
    private String optionValue;

    // 상품 옵션별 고유 코드 (Variant Code)
    @Column(name = "variant_code", length = 50)
    private String variantCode;

    // (카페24) 주문 내 상품 항목 코드 (Item Code)
    @Column(name = "item_code", length = 50)
    private String itemCode;

    // 편의 메서드: 부모 주문 설정
    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }

    // 편의 메서드: 상품 설정
    public void setProduct(Product product) {
        this.product = product;
    }
}
