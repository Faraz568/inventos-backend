package com.inventos.dto.response;

import com.inventos.entity.Purchase;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseResponse {
    private Long          id;
    private Long          productId;
    private String        productName;
    private String        categoryName;
    private String        supplierName;
    private Integer       quantity;
    private BigDecimal    unitCost;
    private BigDecimal    totalCost;
    private String        note;
    private String        status;
    private LocalDateTime purchasedAt;
    private LocalDateTime createdAt;

    public static PurchaseResponse from(Purchase p) {
        return PurchaseResponse.builder()
            .id(p.getId())
            .productId(p.getProduct() != null ? p.getProduct().getId() : null)
            .productName(p.getProduct() != null ? p.getProduct().getName() : null)
            .categoryName(p.getProduct() != null && p.getProduct().getCategory() != null
                ? p.getProduct().getCategory().getName() : null)
            .supplierName(p.getSupplierName())
            .quantity(p.getQuantity())
            .unitCost(p.getUnitCost())
            .totalCost(p.getTotalCost())
            .note(p.getNote())
            .status(p.getStatus())
            .purchasedAt(p.getPurchasedAt())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
