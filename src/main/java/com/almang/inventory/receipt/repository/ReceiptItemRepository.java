package com.almang.inventory.receipt.repository;

import com.almang.inventory.receipt.domain.ReceiptItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    @Query("""
        SELECT receiptItem 
        FROM ReceiptItem receiptItem 
        JOIN FETCH receiptItem.receipt
        JOIN FETCH receiptItem.product
        WHERE receiptItem.receipt.id IN :receiptIds
        ORDER BY receiptItem.receipt.id ASC, receiptItem.id ASC
    """)
    List<ReceiptItem> findAllByReceiptIdInWithProduct(List<Long> receiptIds);
}
