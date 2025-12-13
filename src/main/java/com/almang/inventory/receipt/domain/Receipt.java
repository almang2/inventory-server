package com.almang.inventory.receipt.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.domain.Order;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE receipts SET deleted_at = NOW() WHERE receipt_id = ?")
@Where(clause = "deleted_at IS NULL")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReceiptStatus status;

    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReceiptItem> items = new ArrayList<>();

    public void addItem(ReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }

    public void update(
            ReceiptStatus status, Boolean activated
    ) {
        if (status != null) {
            this.status = status;
        }
        if (activated != null) {
            this.activated = activated;
        }
    }

    public void delete() {
        if (this.status == ReceiptStatus.CONFIRMED) {
            throw new BaseException(ErrorCode.RECEIPT_ALREADY_CONFIRMED);
        }
        if (this.status == ReceiptStatus.CANCELED) {
            throw new BaseException(ErrorCode.RECEIPT_ALREADY_CANCELED);
        }
        this.deletedAt = LocalDateTime.now();
        this.activated = false;
        this.status = ReceiptStatus.CANCELED;
    }

    public void confirm() {
        if (!this.activated || this.status == ReceiptStatus.CANCELED) {
            throw new BaseException(ErrorCode.RECEIPT_ALREADY_CANCELED);
        }
        if (this.status == ReceiptStatus.CONFIRMED) {
            throw new BaseException(ErrorCode.RECEIPT_ALREADY_CONFIRMED);
        }
        this.status = ReceiptStatus.CONFIRMED;
    }
}
