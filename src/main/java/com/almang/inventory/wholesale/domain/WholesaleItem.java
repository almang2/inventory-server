package com.almang.inventory.wholesale.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wholesales_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WholesaleItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesales_id", nullable = false)
    private Wholesale wholesale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    public void setWholesale(Wholesale wholesale) {
        this.wholesale = wholesale;
    }

    public void update(Integer quantity, Integer unitPrice) {
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (unitPrice != null) {
            this.unitPrice = unitPrice;
        }
        if (quantity != null && unitPrice != null) {
            this.amount = quantity * unitPrice;
        }
    }
}

