package com.trading.repository;

import com.trading.entity.BuyerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuyerAddressRepository extends JpaRepository<BuyerAddress, Long> {
    List<BuyerAddress> findByBuyerIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(Long buyerId);
    Optional<BuyerAddress> findByIdAndBuyerId(Long id, Long buyerId);
    long countByBuyerId(Long buyerId);
    Optional<BuyerAddress> findFirstByBuyerIdOrderByUpdatedAtDescIdDesc(Long buyerId);

    @Modifying
    @Query("update BuyerAddress a set a.defaultAddress = false where a.buyerId = :buyerId")
    void clearDefaultByBuyerId(@Param("buyerId") Long buyerId);
}
