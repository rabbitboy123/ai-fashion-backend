package com.fashion.backend.modules.product.dto.response;

import com.fashion.backend.common.constant.GenderTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private String style;
    private String occasion;
    private GenderTarget genderTarget;
    private List<String> availableColors;
    private List<String> availableSizes;
    private Integer totalStock;
}
