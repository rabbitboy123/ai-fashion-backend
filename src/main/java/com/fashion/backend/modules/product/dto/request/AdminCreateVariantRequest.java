package com.fashion.backend.modules.product.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateVariantRequest {

    private Long sizeId;
    private String sizeName;

    private Long colorId;
    private String colorName;
    private String colorHex;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    @Builder.Default
    private Integer stockQuantity = 10;

    @Builder.Default
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    private String sku;
    private String imageUrl;
}
