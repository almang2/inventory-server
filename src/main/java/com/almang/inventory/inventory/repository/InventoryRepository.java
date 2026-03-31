package com.almang.inventory.inventory.repository;

import com.almang.inventory.inventory.domain.Inventory;
import com.almang.inventory.product.domain.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProduct_Id(Long productId);

    Optional<Inventory> findByProduct(Product product);
    
    List<Inventory> findAllByProduct_IdIn(List<Long> productIds);

    @Query(
        value = """
            SELECT inventory
            FROM Inventory inventory
            JOIN FETCH inventory.product product
            WHERE inventory.product.store.id = :storeId
                AND (:q IS NULL OR LOWER(inventory.product.name) LIKE LOWER(CONCAT('%', :q, '%')))
                AND (
                    :scope = 'ALL'
                    OR (:scope = 'DISPLAY' AND inventory.displayStock > 0)
                    OR (:scope = 'WAREHOUSE' AND inventory.warehouseStock > 0)
                    OR (:scope = 'OUTGOING' AND inventory.outgoingReserved > 0)
                    OR (:scope = 'INCOMING' AND inventory.incomingReserved > 0)
                )
        """,
        countQuery = """
            SELECT COUNT(inventory)
            FROM Inventory inventory
            JOIN inventory.product product
            WHERE inventory.product.store.id = :storeId
                AND (:q IS NULL OR LOWER(inventory.product.name) LIKE LOWER(CONCAT('%', :q, '%')))
                AND (
                    :scope = 'ALL'
                    OR (:scope = 'DISPLAY' AND inventory.displayStock > 0)
                    OR (:scope = 'WAREHOUSE' AND inventory.warehouseStock > 0)
                    OR (:scope = 'OUTGOING' AND inventory.outgoingReserved > 0)
                    OR (:scope = 'INCOMING' AND inventory.incomingReserved > 0)
                )
        """
    )
    Page<Inventory> findByFilter(
            @Param("storeId") Long storeId, @Param("scope") String scope, @Param("q") String q, Pageable pageable
    );

    @Query("""
        SELECT inventory 
        FROM Inventory inventory 
        JOIN FETCH inventory.product product
        WHERE product.id in :productIds
    """)
    List<Inventory> findAllWithProductByProductIds(@Param("productIds") List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT inventory 
        FROM Inventory inventory 
        WHERE inventory.product.id = :productId
    """)
    Optional<Inventory> findWithLockByProductId(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT inventory 
        FROM Inventory inventory 
        WHERE inventory.id = :inventoryId
    """)
    Optional<Inventory> findWithLockById(@Param("inventoryId") Long inventoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT inventory 
        FROM Inventory inventory 
        JOIN FETCH inventory.product product 
        WHERE product.id IN :productIds 
        ORDER BY product.id ASC
    """)
    List<Inventory> findAllWithLockByProductIdIn(@Param("productIds") List<Long> productIds);
}
