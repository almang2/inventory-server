package com.almang.inventory.order.template.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.vendor.domain.Vendor;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_template_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    public void updateTemplate(String title, String body, Boolean activated) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (body != null && !body.isBlank()) {
            this.body = body;
        }
        if (activated != null) {
            this.activated = activated;
        }
    }
}
