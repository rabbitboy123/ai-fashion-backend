package com.fashion.backend.modules.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
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

    @Positive(message = "ID size phải lớn hơn 0")
    private Long sizeId;
    @Size(max = 50)
    private String sizeName;

    @Positive(message = "ID màu phải lớn hơn 0")
    private Long colorId;
    @Size(max = 50)
    private String colorName;
    @Pattern(regexp = "^#?(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$", message = "Mã màu phải ở dạng hex")
    private String colorHex;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    @Builder.Default
    private Integer stockQuantity = 10;

    @DecimalMin(value = "0.0", message = "Giá cộng thêm không được âm")
    @Digits(integer = 10, fraction = 2, message = "Giá cộng thêm không đúng định dạng")
    @Builder.Default
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Size(max = 100)
    private String sku;
    @Size(max = 500)
    private String imageUrl;
}
