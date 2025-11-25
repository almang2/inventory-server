package com.almang.inventory.inventory.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.*;
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

    @Column(name = "reorder_trigger_point", precision = 3, scale = 2)
    private BigDecimal reorderTriggerPoint;

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
    public void confirmIncoming(BigDecimal quantity) {
        if (this.incomingReserved.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.INCOMING_STOCK_NOT_ENOUGH);
        }
        this.incomingReserved = this.incomingReserved.subtract(quantity);
        this.warehouseStock = this.warehouseStock.add(quantity);
    }

    // 창고에서 매대로 이동
    public void moveToDisplay(BigDecimal quantity) {
        if (this.warehouseStock.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.WAREHOUSE_STOCK_NOT_ENOUGH);
        }
        this.warehouseStock = this.warehouseStock.subtract(quantity);
        this.displayStock = this.displayStock.add(quantity);
    }

    // 판매(매대 차감)
    public void decreaseDisplay(BigDecimal quantity) {
        if (this.displayStock.compareTo(quantity) < 0) {
            throw new BaseException(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH);
        }
        this.displayStock = this.displayStock.subtract(quantity);
    }
}