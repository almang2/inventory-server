package com.almang.inventory.order.template;

import com.almang.inventory.order.template.domain.OrderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTemplateRepository extends JpaRepository<OrderTemplate, Long> {
}
