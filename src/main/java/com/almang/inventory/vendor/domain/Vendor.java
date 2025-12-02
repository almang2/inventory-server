package com.almang.inventory.vendor.domain;

import com.almang.inventory.global.entity.BaseTimeEntity;
import com.almang.inventory.store.domain.Store;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "vendors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vendors SET deleted_at = NOW() WHERE vendor_id = ?")
@Where(clause = "deleted_at IS NULL")
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

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "email", length = 30)
    private String email;

    @Column(name = "web_page")
    private String webPage;

    @Column(name = "order_method")
    private String orderMethod;

    @Column(name = "note")
    private String note;

    @Column(name = "is_activate", nullable = false)
    private boolean activated;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateBasicInfo(String name, String orderMethod) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (orderMethod != null && !orderMethod.isBlank()) {
            this.orderMethod = orderMethod;
        }
    }

    public void updateContactInfo(VendorChannel channel, String phoneNumber, String email, String webPage) {
        if (channel != null) {
            this.channel = channel;
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            this.phoneNumber = phoneNumber;
        }
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (webPage != null && !webPage.isBlank()) {
            this.webPage = webPage;
        }
    }

    public void updateMeta(String note, Boolean activated) {
        if (note != null) {
            this.note = note;
        }
        if (activated != null) {
            this.activated = activated;
        }
    }
}
