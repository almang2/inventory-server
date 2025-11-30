package com.almang.inventory.retail.repository;

import com.almang.inventory.retail.domain.Retail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailRepository extends JpaRepository<Retail, Long> {
}
