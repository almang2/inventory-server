package com.almang.inventory.retail.repository;

import com.almang.inventory.retail.domain.Retail;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailRepository extends JpaRepository<Retail, Long> {

    // 날짜 범위로 조회 (페이지네이션)
    Page<Retail> findAllByStoreIdAndSoldDateBetween(
            Long storeId, LocalDate start, LocalDate end, Pageable pageable);

    // 특정 날짜 조회 (페이지네이션)
    Page<Retail> findAllByStoreIdAndSoldDate(
            Long storeId, LocalDate soldDate, Pageable pageable);

    // 특정 날짜 조회 (리스트)
    List<Retail> findAllByStoreIdAndSoldDate(Long storeId, LocalDate soldDate);

    // 상품별 + 날짜 범위 조회
    Page<Retail> findAllByStoreIdAndProductIdAndSoldDateBetween(
            Long storeId, Long productId, LocalDate start, LocalDate end, Pageable pageable);

    // 상품별 + 특정 날짜 조회
    List<Retail> findAllByStoreIdAndProductIdAndSoldDate(
            Long storeId, Long productId, LocalDate soldDate);

    // 스토어 없이 날짜만으로 조회 (스토어가 null일 때 사용)
    List<Retail> findAllBySoldDate(LocalDate soldDate);
}
