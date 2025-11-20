package com.almang.inventory.vendor.repository;

import com.almang.inventory.vendor.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
}
