package com.trading.repository;

import com.trading.entity.ShopFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopFavoriteRepository extends JpaRepository<ShopFavorite, Long> {
    Optional<ShopFavorite> findByBuyerIdAndShopId(Long buyerId, Long shopId);
    boolean existsByBuyerIdAndShopId(Long buyerId, Long shopId);
    long countByShopId(Long shopId);
    Page<ShopFavorite> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
}
