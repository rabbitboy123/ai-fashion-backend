package com.fashion.backend.modules.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private Long sizeId;
    private String sizeName;
    private Long colorId;
    private String colorName;
    private String colorHex;
    private String sku;
    private Integer stockQuantity;
    private BigDecimal additionalPrice;
    private BigDecimal finalPrice;
    private String imageUrl;
}
