package com.almang.inventory.order.template.repository;

import com.almang.inventory.order.template.domain.OrderTemplate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTemplateRepository extends JpaRepository<OrderTemplate, Long> {

    // 발주처 기준
    List<OrderTemplate> findAllByVendorId(Long vendorId);

    List<OrderTemplate> findAllByVendorIdAndActivatedTrue(Long vendorId);

    List<OrderTemplate> findAllByVendorIdAndActivatedFalse(Long vendorId);

    // 상점 기준
    Page<OrderTemplate> findAllByVendorStoreId(Long storeId, Pageable pageable);

    Page<OrderTemplate> findAllByVendorStoreIdAndActivatedTrue(Long storeId, Pageable pageable);

    Page<OrderTemplate> findAllByVendorStoreIdAndActivatedFalse(Long storeId, Pageable pageable);

}
