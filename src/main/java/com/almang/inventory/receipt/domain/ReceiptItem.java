package com.almang.inventory.receipt.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.*;

@Entity
@Table(name = "receipt_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReceiptItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "expected_quantity", precision = 8, scale = 3)
    private BigDecimal expectedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "unit_price")
    private Integer unitPrice;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public void update(
            Integer actualQuantity, Integer unitPrice, String note
    ) {
        if (actualQuantity != null) {
            this.actualQuantity = actualQuantity;
        }
        if (unitPrice != null) {
            this.unitPrice = unitPrice;
        }
        if (note != null) {
            this.note = note;
        }
        if (this.actualQuantity != null && this.unitPrice != null) {
            this.amount = this.actualQuantity * this.unitPrice;
        }
    }
}
