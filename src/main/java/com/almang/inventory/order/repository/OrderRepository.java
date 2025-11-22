package com.almang.inventory.order.repository;

import com.almang.inventory.order.domain.Order;
import com.almang.inventory.order.domain.OrderStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 상점 기준 발주 목록
    Page<Order> findAllByStoreId(Long storeId, Pageable pageable);

    // 상점 + 상태 기준
    Page<Order> findAllByStoreIdAndStatus(Long storeId, OrderStatus status, Pageable pageable);

    // 기간 + 상점 기준
    Page<Order> findAllByStoreIdAndCreatedAtBetween(
            Long storeId, LocalDateTime start, LocalDateTime end, Pageable pageable
    );
}
