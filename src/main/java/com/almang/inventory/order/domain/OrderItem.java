package com.almang.inventory.order.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public void setOrder(Order order) {
        this.order = order;
    }

    public void update(Integer quantity, Integer unitPrice, String note) {
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (unitPrice != null) {
            this.unitPrice = unitPrice;
        }
        if (note != null) {
            this.note = note;
        }
        this.amount = this.quantity * this.unitPrice;
    }
}
