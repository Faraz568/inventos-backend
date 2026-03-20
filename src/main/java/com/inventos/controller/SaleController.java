package com.inventos.controller;

import com.inventos.dto.request.SaleRequest;
import com.inventos.dto.response.ApiResponse;
import com.inventos.dto.response.SaleResponse;
import com.inventos.entity.Purchase;
import com.inventos.repository.ProductRepository;
import com.inventos.repository.PurchaseRepository;
import com.inventos.repository.SaleRepository;
import com.inventos.service.SaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService       saleService;
    private final SaleRepository    saleRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;

    
    @Data
    static class BulkSaleRequest {
        @Size(max = 30) private String invoiceNo;
        @Size(max = 150) private String customerName;
        @Size(max = 30)  private String customerPhone;
        @Size(max = 300) private String customerAddress;
        @Size(max = 500) private String note;
        @NotEmpty @Valid  private List<SaleRequest> lines;
    }

    

    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String invoiceNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<SaleResponse> result = (productId != null || invoiceNo != null || from != null || to != null)
            ? saleService.search(productId, invoiceNo, from, to)
            : saleService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(saleService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponse>> create(
            @Valid @RequestBody SaleRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Sale recorded", saleService.create(req, principal.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        saleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Sale deleted", null));
    }

    

    @PostMapping("/bulk")
    @Transactional
    public ResponseEntity<ApiResponse<List<SaleResponse>>> createBulk(
            @Valid @RequestBody BulkSaleRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        List<SaleResponse> results = new ArrayList<>();
        for (SaleRequest line : req.getLines()) {
            
            line.setInvoiceNo(req.getInvoiceNo());
            line.setCustomerName(req.getCustomerName());
            line.setCustomerPhone(req.getCustomerPhone());
            line.setCustomerAddress(req.getCustomerAddress());
            line.setNote(req.getNote());
            if (line.getSoldAt() == null) line.setSoldAt(LocalDateTime.now());
            results.add(saleService.create(line, principal.getUsername()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Invoice " + req.getInvoiceNo() + " created", results));
    }

    

    @GetMapping("/next-invoice-no")
    public ResponseEntity<ApiResponse<String>> nextInvoiceNo() {
        return ResponseEntity.ok(ApiResponse.ok(saleService.nextInvoiceNo()));
    }

    

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SaleService.SaleStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(saleService.getStats()));
    }

    
    
    
    

    @GetMapping("/stock/ledger")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> stockLedger(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,    
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<Map<String, Object>> entries = new ArrayList<>();

        
        saleRepository.findAllByOrderBySoldAtDesc().forEach(s -> {
            if (productId != null && !s.getProduct().getId().equals(productId)) return;
            if (from != null && s.getSoldAt().isBefore(from)) return;
            if (to   != null && s.getSoldAt().isAfter(to))   return;
            if ("in".equals(type)) return;

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id",           "S-" + s.getId());
            e.put("date",         s.getSoldAt());
            e.put("type",         "out");
            e.put("typeLabel",    "Sale");
            e.put("productId",    s.getProduct().getId());
            e.put("productName",  s.getProduct().getName());
            e.put("categoryName", s.getProduct().getCategory() != null ? s.getProduct().getCategory().getName() : "");
            e.put("qty",          -s.getQuantity());
            e.put("qtyAbs",       s.getQuantity());
            e.put("reference",    s.getInvoiceNo() != null ? s.getInvoiceNo() : "SAL-" + s.getId());
            e.put("party",        s.getSoldBy());
            e.put("rate",         s.getUnitPrice());
            e.put("amount",       s.getTotalPrice());
            e.put("note",         s.getNote() != null ? s.getNote() : "");
            entries.add(e);
        });

        
        purchaseRepository.findAllByOrderByPurchasedAtDesc().forEach(p -> {
            if (!"received".equalsIgnoreCase(p.getStatus())) return;
            if (productId != null && !p.getProduct().getId().equals(productId)) return;
            if (from != null && p.getPurchasedAt().isBefore(from)) return;
            if (to   != null && p.getPurchasedAt().isAfter(to))   return;
            if ("out".equals(type)) return;

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id",           "P-" + p.getId());
            e.put("date",         p.getPurchasedAt());
            e.put("type",         "in");
            e.put("typeLabel",    "Purchase");
            e.put("productId",    p.getProduct().getId());
            e.put("productName",  p.getProduct().getName());
            e.put("categoryName", p.getProduct().getCategory() != null ? p.getProduct().getCategory().getName() : "");
            e.put("qty",          p.getQuantity());
            e.put("qtyAbs",       p.getQuantity());
            e.put("reference",    String.format("PO-%04d", p.getId()));
            e.put("party",        p.getSupplierName());
            e.put("rate",         p.getUnitCost());
            e.put("amount",       p.getTotalCost());
            e.put("note",         p.getNote() != null ? p.getNote() : "");
            entries.add(e);
        });

        
        entries.sort(Comparator.comparing(e -> (LocalDateTime) e.get("date")));
        Map<Long, Integer> balance = new HashMap<>();
        for (Map<String, Object> e : entries) {
            Long pid = (Long) e.get("productId");
            int  qty = (int)  e.get("qty");
            balance.merge(pid, qty, Integer::sum);
            e.put("runningBalance", balance.get(pid));
        }
        Collections.reverse(entries);

        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    
    
    
    

    @GetMapping("/stock/balance")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> stockBalance() {

        List<Map<String, Object>> result = productRepository.findByIsActiveTrue(
                org.springframework.data.domain.Pageable.unpaged())
            .getContent()
            .stream()
            .map(p -> {
                
                int totalIn = purchaseRepository.findByProduct_IdOrderByPurchasedAtDesc(p.getId())
                    .stream()
                    .filter(pu -> "received".equalsIgnoreCase(pu.getStatus()))
                    .mapToInt(Purchase::getQuantity)
                    .sum();

                
                Integer totalOut = saleRepository.sumQuantityByProductId(p.getId());
                if (totalOut == null) totalOut = 0;

                
                List<Purchase> received = purchaseRepository.findByProduct_IdOrderByPurchasedAtDesc(p.getId())
                    .stream()
                    .filter(pu -> "received".equalsIgnoreCase(pu.getStatus()))
                    .toList();
                BigDecimal avgCost = received.isEmpty()
                    ? (p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO)
                    : received.stream()
                        .map(pu -> pu.getTotalCost())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(
                            BigDecimal.valueOf(received.stream().mapToInt(Purchase::getQuantity).sum()),
                            2, RoundingMode.HALF_UP
                        );

                BigDecimal qty        = BigDecimal.valueOf(p.getQuantity());
                BigDecimal stockVal   = qty.multiply(avgCost);
                BigDecimal retailVal  = qty.multiply(p.getPrice());
                BigDecimal potProfit  = retailVal.subtract(stockVal);

                String status = p.getQuantity() == 0 ? "OUT_OF_STOCK"
                    : p.getQuantity() <= p.getReorderLevel() ? "LOW_STOCK"
                    : "IN_STOCK";

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",              p.getId());
                row.put("name",            p.getName());
                row.put("sku",             p.getSku() != null ? p.getSku() : "");
                row.put("categoryId",      p.getCategory() != null ? p.getCategory().getId() : null);
                row.put("categoryName",    p.getCategory() != null ? p.getCategory().getName() : "");
                row.put("quantity",        p.getQuantity());
                row.put("reorderLevel",    p.getReorderLevel());
                row.put("stockStatus",     status);
                row.put("price",           p.getPrice());
                row.put("costPrice",       p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
                row.put("avgCost",         avgCost);
                row.put("totalIn",         totalIn);
                row.put("totalOut",        totalOut);
                row.put("stockVal",        stockVal);
                row.put("retailVal",       retailVal);
                row.put("potentialProfit", potProfit);
                row.put("description",     p.getDescription() != null ? p.getDescription() : "");
                return row;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    
    
    
    

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> globalSearch(
            @RequestParam(defaultValue = "") String q) {

        String query = q.toLowerCase().trim();

        List<Map<String, Object>> products = productRepository
            .searchProducts(query.isEmpty() ? null : query, null,
                org.springframework.data.domain.PageRequest.of(0, 20,
                    org.springframework.data.domain.Sort.by("name")))
            .getContent()
            .stream()
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           p.getId());
                m.put("name",         p.getName());
                m.put("sku",          p.getSku() != null ? p.getSku() : "");
                m.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "");
                m.put("quantity",     p.getQuantity());
                m.put("price",        p.getPrice());
                m.put("stockStatus",  p.getStockStatus());
                return m;
            })
            .toList();

        Map<String, Object> result = Map.of("products", products);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
