package com.almang.inventory.wholesale.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wholesales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Wholesale extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WholesaleStatus status;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "wholesale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WholesaleItem> items = new ArrayList<>();

    // Cafe24 주문 ID 저장 (중복 방지용)
    @Column(name = "cafe24_order_id", length = 50, unique = true)
    private String cafe24OrderId;

    public void addItem(WholesaleItem item) {
        items.add(item);
        item.setWholesale(this);
    }

    public void updateStatus(WholesaleStatus status) {
        this.status = status;
    }

    public void updateInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void updateProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}

