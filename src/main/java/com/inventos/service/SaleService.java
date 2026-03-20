package com.inventos.service;

import com.inventos.dto.request.SaleRequest;
import com.inventos.dto.response.SaleResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleService {

    List<SaleResponse> getAll();

    List<SaleResponse> search(Long productId, String invoiceNo,
                               LocalDateTime from, LocalDateTime to);

    SaleResponse getById(Long id);

    
    SaleResponse create(SaleRequest req, String username);

    void delete(Long id);

    
    BigDecimal totalRevenue();

    BigDecimal revenueSince(LocalDateTime from);

    
    String nextInvoiceNo();

    record SaleStats(
        long           total,
        BigDecimal     totalRevenue
    ) {}

    SaleStats getStats();
}
