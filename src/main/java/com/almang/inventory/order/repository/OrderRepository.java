package com.almang.inventory.order.repository;

import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 필터 없음
    Page<Order> findAllByStoreIdAndCreatedAtBetween(
            Long storeId, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 상태 필터
    Page<Order> findAllByStoreIdAndStatusAndCreatedAtBetween(
            Long storeId, OrderStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 발주처 필터
    Page<Order> findAllByStoreIdAndVendorIdAndCreatedAtBetween(
            Long storeId, Long vendorId, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 발주처 + 상태 필터
    Page<Order> findAllByStoreIdAndVendorIdAndStatusAndCreatedAtBetween(
            Long storeId, Long vendorId, OrderStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable
    );
}
