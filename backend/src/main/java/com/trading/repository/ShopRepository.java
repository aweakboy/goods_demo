package com.trading.repository;

import com.trading.entity.Shop;
import com.trading.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findBySellerId(Long sellerId);
    Optional<Shop> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT s FROM Shop s WHERE s.status = 'ACTIVE' AND (:name IS NULL OR s.name LIKE %:name%)")
    List<Shop> searchActive(@Param("name") String name);

    @Query("SELECT s FROM Shop s WHERE (:status IS NULL OR s.status = :status) ORDER BY s.createdAt DESC")
    Page<Shop> findAllByFilter(@Param("status") ProductStatus status, Pageable pageable);
}
