package com.fashion.backend.modules.product.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "ID danh mục không được để trống")
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private Long categoryId;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá sản phẩm phải lớn hơn hoặc bằng 0")
    @Digits(integer = 10, fraction = 2, message = "Giá sản phẩm không đúng định dạng")
    private BigDecimal price;

    @Size(max = 100)
    private String brand;
    @Size(max = 100)
    private String material;

    @Builder.Default
    private GenderTarget genderTarget = GenderTarget.UNISEX;

    @Size(max = 50)
    private String style;
    @Size(max = 50)
    private String occasion;
    @Size(max = 500)
    private String imageUrl;
    private String description;
}
