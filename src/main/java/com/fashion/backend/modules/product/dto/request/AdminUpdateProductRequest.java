package com.fashion.backend.modules.product.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateProductRequest {

    @Size(max = 255)
    @Pattern(regexp = "(?s).*\\S.*", message = "Tên sản phẩm không được để trống")
    private String name;
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private Long categoryId;
    @DecimalMin(value = "0.0", message = "Giá sản phẩm phải lớn hơn hoặc bằng 0")
    @Digits(integer = 10, fraction = 2, message = "Giá sản phẩm không đúng định dạng")
    private BigDecimal price;
    @Size(max = 100)
    private String brand;
    @Size(max = 100)
    private String material;
    private GenderTarget genderTarget;
    @Size(max = 50)
    private String style;
    @Size(max = 50)
    private String occasion;
    @Size(max = 500)
    private String imageUrl;
    private String description;
    private ProductStatus status;
}
