package com.inventos.service.impl;

import com.inventos.dto.request.ProductRequest;
import com.inventos.dto.response.ProductResponse;
import com.inventos.entity.*;
import com.inventos.exception.*;
import com.inventos.repository.*;
import com.inventos.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;

    @Override @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(String name, Long categoryId, Pageable pageable) {
        Page<Product> page = (StringUtils.hasText(name) || categoryId != null)
            ? productRepository.searchProducts(StringUtils.hasText(name) ? name : null, categoryId, pageable)
            : productRepository.findByIsActiveTrue(pageable);
        return page.map(ProductResponse::from);
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(productRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id)));
    }

    @Override @Transactional
    public ProductResponse create(ProductRequest req, String username) {
        if (StringUtils.hasText(req.getSku()) && productRepository.existsBySku(req.getSku()))
            throw new BusinessException("SKU '" + req.getSku() + "' already exists");
        Category cat = categoryRepository.findById(req.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
        User creator = userRepository.findByUsername(username).orElse(null);
        Product p = Product.builder()
            .name(req.getName()).category(cat).quantity(req.getQuantity()).price(req.getPrice())
            .costPrice(req.getCostPrice() != null ? req.getCostPrice() : BigDecimal.ZERO)
            .sku(req.getSku()).reorderLevel(req.getReorderLevel() != null ? req.getReorderLevel() : 10)
            .description(req.getDescription()).isActive(true).createdBy(creator).build();
        return ProductResponse.from(productRepository.save(p));
    }

    @Override @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = productRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (StringUtils.hasText(req.getSku()) && !req.getSku().equals(p.getSku())
                && productRepository.existsBySku(req.getSku()))
            throw new BusinessException("SKU '" + req.getSku() + "' belongs to another product");
        Category cat = categoryRepository.findById(req.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
        p.setName(req.getName()); p.setCategory(cat); p.setQuantity(req.getQuantity());
        p.setPrice(req.getPrice());
        if (req.getCostPrice()   != null) p.setCostPrice(req.getCostPrice());
        if (StringUtils.hasText(req.getSku())) p.setSku(req.getSku());
        if (req.getReorderLevel() != null) p.setReorderLevel(req.getReorderLevel());
        p.setDescription(req.getDescription());
        return ProductResponse.from(productRepository.save(p));
    }

    @Override @Transactional
    public void delete(Long id) {
        Product p = productRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        p.setIsActive(false); productRepository.save(p);
        log.info("Soft-deleted product: {}", id);
    }

    @Override @Transactional(readOnly = true)
    public List<ProductResponse> getLowStock() {
        return productRepository.findLowStockProducts().stream().map(ProductResponse::from).toList();
    }

    @Override @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long total      = productRepository.countByIsActiveTrue();
        long outOfStock = productRepository.countOutOfStock();
        long lowStock   = productRepository.countLowStock();
        BigDecimal totalValue = productRepository.findByIsActiveTrue(Pageable.unpaged())
            .getContent().stream()
            .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardStats(total, outOfStock, lowStock, totalValue);
    }
}
