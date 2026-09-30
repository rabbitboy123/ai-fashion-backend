package com.fashion.backend.modules.ai.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIVariantItemResponse {
    private Long id;
    private String size;
    private String sizeName;
    private String color;
    private String colorName;
    private String colorHex;
    private Integer stockQuantity;
    private BigDecimal additionalPrice;
}
