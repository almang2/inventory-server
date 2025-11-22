package com.almang.inventory.order.template.repository;

import com.almang.inventory.order.template.domain.OrderTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTemplateRepository extends JpaRepository<OrderTemplate, Long> {

    List<OrderTemplate> findAllByVendorId(Long vendorId);

    List<OrderTemplate> findAllByVendorIdAndActivatedTrue(Long vendorId);

    List<OrderTemplate> findAllByVendorIdAndActivatedFalse(Long vendorId);
}
