package com.almang.inventory.wholesale.repository;

import com.almang.inventory.wholesale.domain.Wholesale;
import com.almang.inventory.wholesale.domain.WholesaleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholesaleRepository extends JpaRepository<Wholesale, Long> {

    // 필터 없음
    Page<Wholesale> findAllByStoreIdAndCreatedAtBetween(
            Long storeId, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 상태 필터
    Page<Wholesale> findAllByStoreIdAndStatusAndCreatedAtBetween(
            Long storeId, WholesaleStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 주문서 참조 번호 필터
    Page<Wholesale> findAllByStoreIdAndOrderReferenceContainingAndCreatedAtBetween(
            Long storeId, String orderReference, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 상태 + 주문서 참조 번호 필터
    Page<Wholesale> findAllByStoreIdAndStatusAndOrderReferenceContainingAndCreatedAtBetween(
            Long storeId, WholesaleStatus status, String orderReference, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // 출고일 기준 조회
    Page<Wholesale> findAllByStoreIdAndReleaseDateBetween(
            Long storeId, LocalDate start, LocalDate end, Pageable pageable
    );

    // 상태 + 출고일 기준 조회
    Page<Wholesale> findAllByStoreIdAndStatusAndReleaseDateBetween(
            Long storeId, WholesaleStatus status, LocalDate start, LocalDate end, Pageable pageable
    );
}

