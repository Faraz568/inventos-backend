package com.inventos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank @Size(max = 150)  private String name;
    @NotNull @Positive          private Long categoryId;
    @NotNull @Min(0)            private Integer quantity;
    @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) private BigDecimal price;
    @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) private BigDecimal costPrice;
    @Size(max = 60)             private String sku;
    @Min(0)                     private Integer reorderLevel = 10;
                                private String description;
}
