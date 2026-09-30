package com.fashion.backend.modules.product.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateProductRequest {

    private String name;
    private Long categoryId;
    private BigDecimal price;
    private String brand;
    private String material;
    private GenderTarget genderTarget;
    private String style;
    private String occasion;
    private String imageUrl;
    private String description;
    private ProductStatus status;
}
