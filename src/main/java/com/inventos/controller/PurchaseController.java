package com.inventos.controller;

import com.inventos.dto.request.PurchaseRequest;
import com.inventos.dto.response.ApiResponse;
import com.inventos.dto.response.PurchaseResponse;
import com.inventos.repository.PurchaseRepository;
import com.inventos.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService    purchaseService;
    private final PurchaseRepository purchaseRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<PurchaseResponse> result = (productId != null || status != null || from != null || to != null)
            ? purchaseRepository.search(productId, status, from, to)
                .stream().map(PurchaseResponse::from).toList()
            : purchaseService.getAll();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponse>> create(
            @Valid @RequestBody PurchaseRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Purchase recorded",
                purchaseService.create(req, principal.getUsername())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Purchase updated",
            purchaseService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        purchaseService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Purchase deleted", null));
    }

    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        long total    = purchaseRepository.count();
        long received = purchaseRepository.countByStatus("received");
        long pending  = purchaseRepository.countByStatus("pending");
        BigDecimal totalSpent   = purchaseRepository.sumTotalCost();
        BigDecimal receivedCost = purchaseRepository.sumReceivedCost();

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "total",        total,
            "received",     received,
            "pending",      pending,
            "totalSpent",   totalSpent,
            "receivedCost", receivedCost
        )));
    }
}
