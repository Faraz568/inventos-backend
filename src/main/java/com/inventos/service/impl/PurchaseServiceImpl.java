package com.inventos.service.impl;

import com.inventos.dto.request.PurchaseRequest;
import com.inventos.dto.response.PurchaseResponse;
import com.inventos.entity.*;
import com.inventos.exception.ResourceNotFoundException;
import com.inventos.repository.*;
import com.inventos.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;

    @Override @Transactional(readOnly = true)
    public List<PurchaseResponse> getAll() {
        return purchaseRepository.findAllByOrderByPurchasedAtDesc()
            .stream().map(PurchaseResponse::from).toList();
    }

    @Override @Transactional(readOnly = true)
    public PurchaseResponse getById(Long id) {
        return PurchaseResponse.from(find(id));
    }

    @Override @Transactional
    public PurchaseResponse create(PurchaseRequest req, String username) {
        Product product = productRepository.findByIdAndIsActiveTrue(req.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        User creator = userRepository.findByUsername(username).orElse(null);

        BigDecimal qty  = BigDecimal.valueOf(req.getQuantity());
        BigDecimal total = req.getUnitCost().multiply(qty);

        Purchase purchase = Purchase.builder()
            .product(product)
            .supplierName(req.getSupplierName())
            .quantity(req.getQuantity())
            .unitCost(req.getUnitCost())
            .totalCost(total)
            .note(req.getNote())
            .status(req.getStatus() != null ? req.getStatus() : "received")
            .purchasedAt(req.getPurchasedAt())
            .createdBy(creator)
            .build();

        
        if ("received".equalsIgnoreCase(purchase.getStatus())) {
            product.setQuantity(product.getQuantity() + req.getQuantity());
            productRepository.save(product);
            log.info("Stock updated for product {}: +{}", product.getId(), req.getQuantity());
        }

        Purchase saved = purchaseRepository.save(purchase);
        log.info("Purchase created: id={}, product={}, qty={}", saved.getId(), product.getName(), req.getQuantity());
        return PurchaseResponse.from(saved);
    }

    @Override @Transactional
    public PurchaseResponse update(Long id, PurchaseRequest req) {
        Purchase purchase = find(id);
        Product  product  = productRepository.findByIdAndIsActiveTrue(req.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        String oldStatus = purchase.getStatus();
        String newStatus = req.getStatus() != null ? req.getStatus() : oldStatus;

        
        if ("received".equalsIgnoreCase(oldStatus)) {
            product.setQuantity(Math.max(0, product.getQuantity() - purchase.getQuantity()));
        }

        
        if ("received".equalsIgnoreCase(newStatus)) {
            product.setQuantity(product.getQuantity() + req.getQuantity());
        }

        productRepository.save(product);

        BigDecimal total = req.getUnitCost().multiply(BigDecimal.valueOf(req.getQuantity()));
        purchase.setProduct(product);
        purchase.setSupplierName(req.getSupplierName());
        purchase.setQuantity(req.getQuantity());
        purchase.setUnitCost(req.getUnitCost());
        purchase.setTotalCost(total);
        purchase.setNote(req.getNote());
        purchase.setStatus(newStatus);
        purchase.setPurchasedAt(req.getPurchasedAt());

        return PurchaseResponse.from(purchaseRepository.save(purchase));
    }

    @Override @Transactional
    public void delete(Long id) {
        Purchase purchase = find(id);

        
        if ("received".equalsIgnoreCase(purchase.getStatus())) {
            Product product = purchase.getProduct();
            product.setQuantity(Math.max(0, product.getQuantity() - purchase.getQuantity()));
            productRepository.save(product);
            log.info("Stock reversed for product {}: -{}", product.getId(), purchase.getQuantity());
        }

        purchaseRepository.delete(purchase);
        log.info("Purchase deleted: id={}", id);
    }

    private Purchase find(Long id) {
        return purchaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase", id));
    }
}
