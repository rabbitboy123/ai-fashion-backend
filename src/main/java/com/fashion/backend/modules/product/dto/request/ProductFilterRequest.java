package com.fashion.backend.modules.product.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {
    @Size(max = 200)
    private String keyword;
    @Positive
    private Long categoryId;
    @DecimalMin("0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal minPrice;
    @DecimalMin("0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal maxPrice;
    @Positive
    private Long colorId;
    @Positive
    private Long sizeId;
    @Size(max = 50)
    private String style;
    @Size(max = 50)
    private String occasion;
    private GenderTarget genderTarget;

    @Builder.Default
    @PositiveOrZero
    private Integer page = 0;

    @Builder.Default
    @Positive
    @Max(100)
    private Integer size = 12;

    @Builder.Default
    @Pattern(regexp = "(?i)newest|priceasc|pricedesc", message = "Kiểu sắp xếp không hợp lệ")
    private String sortBy = "newest"; // newest, priceAsc, priceDesc

    @AssertTrue(message = "Giá tối thiểu không được lớn hơn giá tối đa")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
