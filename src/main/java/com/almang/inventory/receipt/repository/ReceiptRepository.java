package com.almang.inventory.receipt.repository;

import com.almang.inventory.receipt.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
}
