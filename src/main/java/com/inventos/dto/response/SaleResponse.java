package com.inventos.dto.response;

import com.inventos.entity.Sale;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleResponse {

    private Long          id;
    private String        invoiceNo;
    private String        customerName;
    private String        customerPhone;
    private String        customerAddress;
    private Long          productId;
    private String        productName;
    private String        categoryName;
    private Integer       quantity;
    private BigDecimal    unitPrice;
    private BigDecimal    totalPrice;
    private String        soldBy;
    private String        note;
    private LocalDateTime soldAt;
    private LocalDateTime createdAt;

    public static SaleResponse from(Sale s) {
        return SaleResponse.builder()
            .id(s.getId())
            .invoiceNo(s.getInvoiceNo())
            .customerName(s.getCustomerName())
            .customerPhone(s.getCustomerPhone())
            .customerAddress(s.getCustomerAddress())
            .productId(s.getProduct() != null ? s.getProduct().getId() : null)
            .productName(s.getProduct() != null ? s.getProduct().getName() : null)
            .categoryName(s.getProduct() != null && s.getProduct().getCategory() != null
                ? s.getProduct().getCategory().getName() : null)
            .quantity(s.getQuantity())
            .unitPrice(s.getUnitPrice())
            .totalPrice(s.getTotalPrice())
            .soldBy(s.getSoldBy())
            .note(s.getNote())
            .soldAt(s.getSoldAt())
            .createdAt(s.getCreatedAt())
            .build();
    }
}
