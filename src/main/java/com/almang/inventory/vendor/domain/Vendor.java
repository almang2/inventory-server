package com.almang.inventory.vendor.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vendors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Vendor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private VendorChannel channel;

    @Column(name = "contact_point", length = 30, nullable = false)
    private String contactPoint;

    @Column(name = "note")
    private String note;

    @Column(name = "is_activate", nullable = false)
    private boolean isActivate;
}
