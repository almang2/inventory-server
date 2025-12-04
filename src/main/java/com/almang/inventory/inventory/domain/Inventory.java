package com.almang.inventory.inventory.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "inventories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_product",
                        columnNames = {"product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Inventory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    // 논리적으로는 1:1, 구현은 ManyToOne + unique
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "display_stock", precision = 10, scale = 3, nullable = false)
    private BigDecimal displayStock;

    @Column(name = "warehouse_stock", precision = 10, scale = 3, nullable = false)
    private BigDecimal warehouseStock;

    @Column(name = "outgoing_reserved", precision = 10, scale = 3, nullable = false)
    private BigDecimal outgoingReserved;

    @Column(name = "incoming_reserved", precision = 10, scale = 3, nullable = false)
    private BigDecimal incomingReserved;

    @Column(name = "reorder_trigger_point", precision = 10, scale = 3, nullable = false)
    private BigDecimal reorderTriggerPoint;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 가용 재고 조회 (창고 재고 - 출고 예정 수량)
     * 실제로 출고 가능한 재고 수량을 반환합니다.
     */
    public BigDecimal getAvailableStock() {
        return this.warehouseStock.subtract(this.outgoingReserved);
    }

    // 입고 예정 추가
    public void increaseIncoming(BigDecimal quantity) {
        this.incomingReserved = this.incomingReserved.add(quantity);
    }

    // 입고 예정 차감
    public void decreaseIncoming(BigDecimal quantity) {
        if (this.incomingReserved.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.INCOMING_STOCK_NOT_ENOUGH);
        }
        this.incomingReserved = this.incomingReserved.subtract(quantity);
    }

    // 입고 확정
    public void confirmIncoming(BigDecimal expected, BigDecimal actual) {
        if (this.incomingReserved.compareTo(expected) < 0) {
            throw new BaseException(ErrorCode.INCOMING_STOCK_NOT_ENOUGH);
        }
        this.incomingReserved = this.incomingReserved.subtract(expected);
        this.warehouseStock = this.warehouseStock.add(actual);
    }

    // 창고에서 매대로 이동
    public void moveWarehouseToDisplay(BigDecimal quantity) {
        if (this.warehouseStock.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH);
        }
        this.warehouseStock = this.warehouseStock.subtract(quantity);
        this.displayStock = this.displayStock.add(quantity);
    }

    // 매대에서 창고로 이동
    public void moveDisplayToWarehouse(BigDecimal quantity) {
        if (this.displayStock.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH);
        }
        this.displayStock = this.displayStock.subtract(quantity);
        this.warehouseStock = this.warehouseStock.add(quantity);
    }

    // 판매(매대 차감)
    public void decreaseDisplay(BigDecimal quantity) {
        if (this.displayStock.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH);
        }
        this.displayStock = this.displayStock.subtract(quantity);
    }

    public void updateManually(
            BigDecimal displayStock,
            BigDecimal warehouseStock,
            BigDecimal outgoingReserved,
            BigDecimal incomingReserved,
            BigDecimal reorderTriggerPoint
    ) {
        if (displayStock != null) {
            this.displayStock = displayStock;
        }
        if (warehouseStock != null) {
            this.warehouseStock = warehouseStock;
        }
        if (outgoingReserved != null) {
            this.outgoingReserved = outgoingReserved;
        }
        if (incomingReserved != null) {
            this.incomingReserved = incomingReserved;
        }
        if (reorderTriggerPoint != null) {
            this.reorderTriggerPoint = reorderTriggerPoint;
        }
    }

    // 출고 예정 추가
    public void increaseOutgoing(BigDecimal quantity) {
        this.outgoingReserved = this.outgoingReserved.add(quantity);
    }

    // 출고 예정 차감
    public void decreaseOutgoing(BigDecimal quantity) {
        if (this.outgoingReserved.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH);
        }
        this.outgoingReserved = this.outgoingReserved.subtract(quantity);
    }

    // 출고 확정 (출고 예정 차감 + 창고 재고 차감)
    public void confirmOutgoing(BigDecimal quantity) {
        if (this.outgoingReserved.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH);
        }
        if (this.warehouseStock.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH);
        }
        this.outgoingReserved = this.outgoingReserved.subtract(quantity);
        this.warehouseStock = this.warehouseStock.subtract(quantity);
    }

    // 출고 취소 (출고 예정 차감만)
    public void cancelOutgoing(BigDecimal quantity) {
        if (this.outgoingReserved.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH);
        }
        this.outgoingReserved = this.outgoingReserved.subtract(quantity);
    }
}