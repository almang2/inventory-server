package com.almang.inventory.retail.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "retails",
        indexes = {
                @Index(name = "idx_store_sold_date", columnList = "store_id,sold_date"),
                @Index(name = "idx_sold_date", columnList = "sold_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Retail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retail_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;  // 판매 시점의 상품 코드 (POS에서 저장된 값)

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;  // 판매 시점의 상품명 (POS에서 저장된 값)

    @Column(name = "sold_date", nullable = false)
    private LocalDate soldDate;  // 판매일자 (날짜별 그룹화용)

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "actual_sales")
    private Integer actualSales;  // 실매출 (원)

    public void updateQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void updateActualSales(Integer actualSales) {
        this.actualSales = actualSales;
    }
}
