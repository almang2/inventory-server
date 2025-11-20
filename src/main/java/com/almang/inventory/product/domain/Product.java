package com.almang.inventory.product.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.vendor.domain.Vendor;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "code", length = 30, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private ProductUnit unit;

    @Column(name = "box_weight_g", precision = 8, scale = 3)
    private BigDecimal boxWeightG;

    @Column(name = "unit_per_box")
    private Integer unitPerBox;

    @Column(name = "unit_weight_g", precision = 8, scale = 3)
    private BigDecimal unitWeightG;

    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    @Column(name = "cost_price")
    private int costPrice;

    @Column(name = "retail_price")
    private int retailPrice;

    @Column(name = "wholesale_price")
    private int wholesalePrice;

    public void updateVendor(Vendor vendor) {
        if (!this.vendor.getId().equals(vendor.getId())) {
            this.vendor = vendor;
        }
    }

    public void updateBasicInfo(String name, String code, ProductUnit unit) {
        if (name != null) {
            this.name = name;
        }
        if (code != null) {
            this.code = code;
        }
        if (unit != null) {
            this.unit = unit;
        }
    }

    public void updateWeights(BigDecimal boxWeightG, Integer unitPerBox, BigDecimal unitWeightG) {
        if (boxWeightG != null) {
            this.boxWeightG = boxWeightG;
        }
        if (unitPerBox != null) {
            this.unitPerBox = unitPerBox;
        }
        if (unitWeightG != null) {
            this.unitWeightG = unitWeightG;
        }
    }

    public void updatePrices(Integer costPrice, Integer retailPrice, Integer wholesalePrice) {
        if (costPrice != null) {
            this.costPrice = costPrice;
        }
        if (retailPrice != null) {
            this.retailPrice = retailPrice;
        }
        if (wholesalePrice != null) {
            this.wholesalePrice = wholesalePrice;
        }
    }

    public void updateActivation(Boolean activated) {
        if (activated != null) {
            this.activated = activated;
        }
    }
}
