package com.almang.inventory.wholesale.repository;

import com.almang.inventory.wholesale.domain.WholesaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholesaleItemRepository extends JpaRepository<WholesaleItem, Long> {
}

