package com.almang.inventory.customerorder.repository;

import com.almang.inventory.customerorder.domain.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // 카페24 주문 ID로 고객 주문을 조회하는 메서드
    Optional<CustomerOrder> findByCafe24OrderId(String cafe24OrderId);
}
