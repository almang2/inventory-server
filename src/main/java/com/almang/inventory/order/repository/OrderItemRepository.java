package com.almang.inventory.order.repository;

import com.almang.inventory.order.domain.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 특정 발주의 상세 목록
    List<OrderItem> findAllByOrderId(Long orderId);

    // 특정 제품이 포함된 모든 발주 상세
    List<OrderItem> findAllByProductId(Long productId);

    // 발주 + 상품 조합으로 단건 조회 필요할 때
    OrderItem findByOrderIdAndProductId(Long orderId, Long productId);
}
