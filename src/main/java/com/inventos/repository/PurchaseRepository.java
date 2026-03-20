package com.inventos.repository;

import com.inventos.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findAllByOrderByPurchasedAtDesc();

    List<Purchase> findByProduct_IdOrderByPurchasedAtDesc(Long productId);

    List<Purchase> findByStatusOrderByPurchasedAtDesc(String status);

    
    @Query("""
        SELECT p FROM Purchase p
        WHERE (:productId IS NULL OR p.product.id = :productId)
          AND (:status    IS NULL OR p.status = :status)
          AND (:from      IS NULL OR p.purchasedAt >= :from)
          AND (:to        IS NULL OR p.purchasedAt <= :to)
        ORDER BY p.purchasedAt DESC
        """)
    List<Purchase> search(
        @Param("productId") Long productId,
        @Param("status")    String status,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );

    
    @Query("SELECT COALESCE(SUM(p.totalCost), 0) FROM Purchase p")
    BigDecimal sumTotalCost();

    @Query("SELECT COALESCE(SUM(p.totalCost), 0) FROM Purchase p WHERE p.status = 'received'")
    BigDecimal sumReceivedCost();

    long countByStatus(String status);

    
    @Query("""
        SELECT COALESCE(SUM(p.quantity), 0) FROM Purchase p
        WHERE p.product.id = :productId AND p.status = 'received'
        """)
    Integer sumReceivedQuantityByProductId(@Param("productId") Long productId);
}
