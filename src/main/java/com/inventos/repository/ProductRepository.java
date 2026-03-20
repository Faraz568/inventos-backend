package com.inventos.repository;

import com.inventos.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsActiveTrue(Pageable pageable);
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id AND p.isActive = true")
    Optional<Product> findByIdAndIsActiveTrue(@Param("id") Long id);
    boolean existsBySku(String sku);

    @Query("""
        SELECT p FROM Product p
        WHERE p.isActive = true
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:name,'%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
        """)
    Page<Product> searchProducts(@Param("name") String name, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    List<Product> findLowStockProducts();

    long countByIsActiveTrue();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.quantity = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.quantity > 0 AND p.quantity <= p.reorderLevel")
    long countLowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.category.id = :categoryId")
    long countActiveByCategoryId(@Param("categoryId") Long categoryId);
}
