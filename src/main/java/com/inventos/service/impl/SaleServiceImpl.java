package com.inventos.service.impl;

import com.inventos.dto.request.SaleRequest;
import com.inventos.dto.response.SaleResponse;
import com.inventos.entity.*;
import com.inventos.exception.BusinessException;
import com.inventos.exception.ResourceNotFoundException;
import com.inventos.repository.*;
import com.inventos.service.SaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class SaleServiceImpl implements SaleService {

    private final SaleRepository    saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;

    
    private final Object  lock    = new Object();

    @Override @Transactional(readOnly = true)
    public List<SaleResponse> getAll() {
        return saleRepository.findAllByOrderBySoldAtDesc()
            .stream().map(SaleResponse::from).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<SaleResponse> search(Long productId, String invoiceNo,
                                      LocalDateTime from, LocalDateTime to) {
        return saleRepository.search(productId, invoiceNo, from, to)
            .stream().map(SaleResponse::from).toList();
    }

    @Override @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        return SaleResponse.from(find(id));
    }

    @Override @Transactional
    public SaleResponse create(SaleRequest req, String username) {
        Product product = productRepository.findByIdAndIsActiveTrue(req.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        
        if (product.getQuantity() < req.getQuantity()) {
            throw new BusinessException(
                "Insufficient stock for '" + product.getName() +
                "'. Available: " + product.getQuantity() +
                ", requested: " + req.getQuantity());
        }

        User creator = userRepository.findByUsername(username).orElse(null);

        
        String invoiceNo = StringUtils.hasText(req.getInvoiceNo())
            ? req.getInvoiceNo()
            : nextInvoiceNo();

        BigDecimal qty   = BigDecimal.valueOf(req.getQuantity());
        BigDecimal total = req.getUnitPrice().multiply(qty);

        Sale sale = Sale.builder()
            .invoiceNo(invoiceNo)
            .customerName(req.getCustomerName())
            .customerPhone(req.getCustomerPhone())
            .customerAddress(req.getCustomerAddress())
            .product(product)
            .quantity(req.getQuantity())
            .unitPrice(req.getUnitPrice())
            .totalPrice(total)
            .soldBy(username)
            .note(req.getNote())
            .soldAt(req.getSoldAt() != null ? req.getSoldAt() : LocalDateTime.now())
            .createdBy(creator)
            .build();

        
        product.setQuantity(product.getQuantity() - req.getQuantity());
        productRepository.save(product);

        Sale saved = saleRepository.save(sale);
        log.info("Sale created: id={}, invoice={}, product={}, qty={}",
            saved.getId(), invoiceNo, product.getName(), req.getQuantity());
        return SaleResponse.from(saved);
    }

    @Override @Transactional
    public void delete(Long id) {
        Sale sale = find(id);

        
        Product product = sale.getProduct();
        product.setQuantity(product.getQuantity() + sale.getQuantity());
        productRepository.save(product);

        saleRepository.delete(sale);
        log.info("Sale deleted: id={}, stock restored for product={}", id, product.getId());
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal totalRevenue() {
        return saleRepository.sumTotalRevenue();
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal revenueSince(LocalDateTime from) {
        return saleRepository.sumRevenueSince(from);
    }

    @Override
    public String nextInvoiceNo() {
        synchronized (lock) {
            
            long next = saleRepository.maxInvoiceId().orElse(0L) + 1;
            return String.format("INV-%04d", next);
        }
    }

    @Override @Transactional(readOnly = true)
    public SaleStats getStats() {
        long count  = saleRepository.count();
        BigDecimal  rev = saleRepository.sumTotalRevenue();
        return new SaleStats(count, rev);
    }

    private Sale find(Long id) {
        return saleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }
}
