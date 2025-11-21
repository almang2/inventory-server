package com.almang.inventory.vendor.repository;

import com.almang.inventory.vendor.domain.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Page<Vendor> findAllByStoreId(Long storeId, Pageable pageable);

    Page<Vendor> findAllByStoreIdAndActivatedTrue(
            Long storeId, Pageable pageable
    );

    Page<Vendor> findAllByStoreIdAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );

    Page<Vendor> findAllByStoreIdAndActivatedTrueAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );
}
