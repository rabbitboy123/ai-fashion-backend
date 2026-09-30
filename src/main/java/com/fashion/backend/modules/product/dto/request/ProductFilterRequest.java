package com.fashion.backend.modules.product.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {
    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long colorId;
    private Long sizeId;
    private String style;
    private String occasion;
    private GenderTarget genderTarget;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 12;

    @Builder.Default
    private String sortBy = "newest"; // newest, priceAsc, priceDesc
}
