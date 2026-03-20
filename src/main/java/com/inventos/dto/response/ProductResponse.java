package com.inventos.dto.response;

import com.inventos.entity.Product;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductResponse {
    private Long          id;
    private String        name;
    private Long          categoryId;
    private String        categoryName;
    private Integer       quantity;
    private BigDecimal    price;
    private BigDecimal    costPrice;
    private String        sku;
    private Integer       reorderLevel;
    private String        description;
    private String        stockStatus;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
            .id(p.getId()).name(p.getName())
            .categoryId(p.getCategory()   != null ? p.getCategory().getId()   : null)
            .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
            .quantity(p.getQuantity()).price(p.getPrice()).costPrice(p.getCostPrice())
            .sku(p.getSku()).reorderLevel(p.getReorderLevel()).description(p.getDescription())
            .stockStatus(p.getStockStatus()).isActive(p.getIsActive())
            .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
