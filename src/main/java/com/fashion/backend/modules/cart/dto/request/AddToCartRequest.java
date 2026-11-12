package com.fashion.backend.modules.cart.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @NotNull(message = "ID biến thể sản phẩm không được để trống")
    @Positive(message = "ID biến thể sản phẩm phải lớn hơn 0")
    private Long productVariantId;

    @Positive(message = "Số lượng phải lớn hơn 0")
    @Builder.Default
    private Integer quantity = 1;
}
