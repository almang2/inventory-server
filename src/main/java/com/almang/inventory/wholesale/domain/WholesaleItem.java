package com.almang.inventory.wholesale.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "wholesale_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WholesaleItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wholesale_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesale_id", nullable = false)
    private Wholesale wholesale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price")
    private Integer unitPrice;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "insufficient_stock", nullable = false)
    @Builder.Default
    private Boolean insufficientStock = false;

    public void setWholesale(Wholesale wholesale) {
        this.wholesale = wholesale;
    }

    public void update(BigDecimal quantity, Integer unitPrice, String note) {
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (unitPrice != null) {
            this.unitPrice = unitPrice;
        }
        if (note != null) {
            this.note = note;
        }
        if (this.quantity != null && this.unitPrice != null) {
            this.amount = this.quantity.multiply(BigDecimal.valueOf(this.unitPrice)).intValue();
        }
    }
    
    public void setInsufficientStock(Boolean insufficientStock) {
        this.insufficientStock = insufficientStock;
    }
}

