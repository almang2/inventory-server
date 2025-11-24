package com.almang.inventory.receipt.repository;

import com.almang.inventory.receipt.domain.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
}
