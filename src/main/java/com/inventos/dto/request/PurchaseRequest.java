package com.inventos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseRequest {

    @NotNull
    private Long productId;

    @NotBlank @Size(max = 150)
    private String supplierName;

    @NotNull @Min(1)
    private Integer quantity;

    @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2)
    private BigDecimal unitCost;

    @Size(max = 500)
    private String note;

    @NotBlank
    private String status = "received";

    @NotNull
    private LocalDateTime purchasedAt;
}
