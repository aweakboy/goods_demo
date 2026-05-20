package com.trading.repository;

import com.trading.entity.Product;
import com.trading.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p JOIN Shop s ON p.sellerId = s.sellerId" +
           " WHERE p.status = 'ACTIVE' AND s.status = 'ACTIVE'" +
           " AND (:categoryId IS NULL OR p.categoryId = :categoryId)" +
           " AND (:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<Product> searchActive(@Param("categoryId") Long categoryId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    List<Product> findBySellerId(Long sellerId);

    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("SELECT p FROM Product p JOIN User u ON p.sellerId = u.id LEFT JOIN Shop s ON p.sellerId = s.sellerId" +
           " WHERE (:status IS NULL OR p.status = :status)" +
           " AND (:sellerName IS NULL OR u.username LIKE %:sellerName%)" +
           " AND (:shopName IS NULL OR s.name LIKE %:shopName%)")
    Page<Product> findByAdminFilters(@Param("status") ProductStatus status,
                                     @Param("sellerName") String sellerName,
                                     @Param("shopName") String shopName,
                                     Pageable pageable);

    long countByCategoryId(Long categoryId);

    long countByStatus(ProductStatus status);

    long countBySellerId(Long sellerId);

    Page<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status, Pageable pageable);
}
