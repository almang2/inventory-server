package com.almang.inventory.wholesale.repository;

import com.almang.inventory.wholesale.domain.Wholesale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WholesaleRepository extends JpaRepository<Wholesale, Long> {
    Optional<Wholesale> findByCafe24OrderId(String cafe24OrderId);
}

