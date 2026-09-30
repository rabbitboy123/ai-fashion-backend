package com.fashion.backend.modules.product.dto.response;

import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String brand;
    private String material;
    private GenderTarget genderTarget;
    private String style;
    private String occasion;
    private String imageUrl;
    private ProductStatus status;
    private CategoryResponse category;
    private List<ProductVariantResponse> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
