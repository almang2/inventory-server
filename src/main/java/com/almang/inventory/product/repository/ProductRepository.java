package com.almang.inventory.product.repository;

import com.almang.inventory.product.domain.Product;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"store", "vendor"})
    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);

    @EntityGraph(attributePaths = {"store", "vendor"})
    Page<Product> findAllByStoreIdAndActivatedTrue(Long storeId, Pageable pageable);

    @EntityGraph(attributePaths = {"store", "vendor"})
    Page<Product> findAllByStoreIdAndActivatedFalse(Long storeId, Pageable pageable);

    @EntityGraph(attributePaths = {"store", "vendor"})
    Page<Product> findAllByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"store", "vendor"})
    Page<Product> findAllByStoreIdAndActivatedTrueAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );

    @EntityGraph(attributePaths = {"store", "vendor"})
    Page<Product> findAllByStoreIdAndActivatedFalseAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );

    boolean existsByVendorId(Long vendorId);

    List<Product> findByStoreIdAndVendorId(Long storeId, Long vendorId);

    // 상품 코드로 상품 찾기 (카페24 주문 처리용)
    Optional<Product> findByCode(String code);

    List<Product> findByStoreIdAndCodeIn(Long storeId, Collection<String> codes);
}
