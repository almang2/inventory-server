package com.almang.inventory.product.repository;

import com.almang.inventory.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);

    Page<Product> findAllByStoreIdAndActivateIsTrue(
            Long storeId, Pageable pageable
    );

    Page<Product> findAllByStoreIdAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );

    Page<Product> findAllByStoreIdAndActivateIsTrueAndNameContainingIgnoreCase(
            Long storeId, String name, Pageable pageable
    );
}
