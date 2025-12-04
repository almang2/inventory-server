package com.almang.inventory.wholesale.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "wholesales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Wholesale extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wholesale_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "order_reference", length = 100)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private WholesaleStatus status;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    @OneToMany(mappedBy = "wholesale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WholesaleItem> items = new ArrayList<>();

    public void addItem(WholesaleItem item) {
        items.add(item);
        item.setWholesale(this);
    }

    public void confirm() {
        confirm(LocalDate.now());
    }

    public void confirm(LocalDate releaseDate) {
        if (!this.activated || this.status == WholesaleStatus.CANCELED) {
            throw new BaseException(ErrorCode.WHOLESALE_ALREADY_CANCELED);
        }
        if (this.status == WholesaleStatus.CONFIRMED) {
            throw new BaseException(ErrorCode.WHOLESALE_ALREADY_CONFIRMED);
        }
        this.status = WholesaleStatus.CONFIRMED;
        this.releaseDate = releaseDate != null ? releaseDate : LocalDate.now();
    }

    public void cancel() {
        if (this.status == WholesaleStatus.CONFIRMED) {
            throw new BaseException(ErrorCode.WHOLESALE_ALREADY_CONFIRMED);
        }
        if (this.status == WholesaleStatus.CANCELED) {
            throw new BaseException(ErrorCode.WHOLESALE_ALREADY_CANCELED);
        }
        this.status = WholesaleStatus.CANCELED;
        this.activated = false;
    }

    public void updateOrderReference(String orderReference) {
        if (this.status != WholesaleStatus.PENDING) {
            throw new BaseException(ErrorCode.WHOLESALE_ALREADY_CONFIRMED,
                    "출고 대기 상태인 경우에만 수정할 수 있습니다.");
        }
        this.orderReference = orderReference;
    }
}

