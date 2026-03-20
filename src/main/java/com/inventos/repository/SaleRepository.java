package com.inventos.repository;

import com.inventos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT s FROM Sale s JOIN FETCH s.product p JOIN FETCH p.category ORDER BY s.soldAt DESC")
    List<Sale> findAllByOrderBySoldAtDesc();

    @Query("SELECT s FROM Sale s JOIN FETCH s.product p JOIN FETCH p.category WHERE p.id = :productId ORDER BY s.soldAt DESC")
    List<Sale> findByProduct_IdOrderBySoldAtDesc(@Param("productId") Long productId);

    @Query("SELECT s FROM Sale s JOIN FETCH s.product p JOIN FETCH p.category WHERE s.invoiceNo = :invoiceNo ORDER BY s.soldAt DESC")
    List<Sale> findByInvoiceNoOrderBySoldAtDesc(@Param("invoiceNo") String invoiceNo);

    @Query("""
        SELECT s FROM Sale s JOIN FETCH s.product p JOIN FETCH p.category
        WHERE (:productId IS NULL OR p.id = :productId)
          AND (:invoiceNo IS NULL OR s.invoiceNo = :invoiceNo)
          AND (:from IS NULL OR s.soldAt >= :from)
          AND (:to   IS NULL OR s.soldAt <= :to)
        ORDER BY s.soldAt DESC
        """)
    List<Sale> search(
        @Param("productId") Long productId,
        @Param("invoiceNo") String invoiceNo,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );

    
    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM Sale s")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM Sale s WHERE s.soldAt >= :from")
    BigDecimal sumRevenueSince(@Param("from") LocalDateTime from);

    long countByProduct_Id(Long productId);

    
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.product.id = :productId")
    Integer sumQuantityByProductId(@Param("productId") Long productId);

    
    @Query("SELECT MAX(s.id) FROM Sale s WHERE s.invoiceNo IS NOT NULL")
    Optional<Long> maxInvoiceId();
}
