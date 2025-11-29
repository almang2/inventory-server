package com.almang.inventory.product.repository;

import com.almang.inventory.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);

    Page<Product> findAllByStoreIdAndActivatedTrue(Long storeId, Pageable pageable);

    Page<Product> findAllByStoreIdAndActivatedFalse(Long storeId, Pageable pageable);

    Page<Product> findAllByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);

    Page<Product> findAllByStoreIdAndActivatedTrueAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );

    Page<Product> findAllByStoreIdAndActivatedFalseAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );

    Optional<Product> findByCode(String code);
}
