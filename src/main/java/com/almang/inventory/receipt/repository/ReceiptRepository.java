package com.almang.inventory.receipt.repository;

import com.almang.inventory.receipt.domain.Receipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByOrder_Id(Long orderId);
}
