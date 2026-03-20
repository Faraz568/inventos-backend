package com.inventos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SaleRequest {

    @Size(max = 30)
    private String invoiceNo;

    @Size(max = 150)
    private String customerName;

    @Size(max = 30)
    private String customerPhone;

    @Size(max = 300)
    private String customerAddress;

    @NotNull
    private Long productId;

    @NotNull @Min(1)
    private Integer quantity;

    @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;

    @Size(max = 500)
    private String note;

    
    private LocalDateTime soldAt;
}
