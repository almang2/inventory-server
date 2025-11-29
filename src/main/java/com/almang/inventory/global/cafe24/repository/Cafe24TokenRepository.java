package com.almang.inventory.global.cafe24.repository;

import com.almang.inventory.global.cafe24.domain.Cafe24Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Cafe24TokenRepository extends JpaRepository<Cafe24Token, Long> {
    Optional<Cafe24Token> findByMallIdAndActiveTrue(String mallId);
    
    Optional<Cafe24Token> findByMallId(String mallId);
}

