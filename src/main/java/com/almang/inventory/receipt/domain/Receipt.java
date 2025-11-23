package com.almang.inventory.receipt.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Receipt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "total_box_count", nullable = false)
    private Integer totalBoxCount;

    @Column(name = "total_weight_g", precision = 8, scale = 3)
    private BigDecimal totalWeightG;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReceiptStatus status;

    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReceiptItem> items = new ArrayList<>();

    public void addItem(ReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }

    public void updateTotalBoxCount(Integer totalBoxCount) {
        if (totalBoxCount != null) {
            this.totalBoxCount = totalBoxCount;
        }
    }

    public void updateTotalWeightG(BigDecimal totalWeightG) {
        this.totalWeightG = totalWeightG;
    }

    public void updateReceiptStatus(ReceiptStatus status) {
        this.status = status;
    }

    public void deactivate() {
        this.activated = false;
        this.status = ReceiptStatus.CANCELED;
    }
}
