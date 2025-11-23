package com.almang.inventory.receipt.repository;

import com.almang.inventory.receipt.domain.Receipt;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByOrder_Id(Long orderId);

    Page<Receipt> findAllByStoreIdAndReceiptDateBetween(
            Long storeId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Receipt> findAllByStoreIdAndStatusAndReceiptDateBetween(
            Long storeId, ReceiptStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Receipt> findAllByStoreIdAndOrderVendorIdAndReceiptDateBetween(
            Long storeId, Long vendorId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Receipt> findAllByStoreIdAndOrderVendorIdAndStatusAndReceiptDateBetween(
            Long storeId, Long vendorId, ReceiptStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);

}
