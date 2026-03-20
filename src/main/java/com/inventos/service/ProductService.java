package com.inventos.service;

import com.inventos.dto.request.ProductRequest;
import com.inventos.dto.response.ProductResponse;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    Page<ProductResponse> getAll(String name, Long categoryId, Pageable pageable);
    ProductResponse getById(Long id);
    ProductResponse create(ProductRequest req, String username);
    ProductResponse update(Long id, ProductRequest req);
    void delete(Long id);
    List<ProductResponse> getLowStock();
    DashboardStats getDashboardStats();

    record DashboardStats(long totalProducts, long outOfStock, long lowStock, BigDecimal totalInventoryValue) {}
}
