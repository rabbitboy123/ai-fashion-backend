package com.fashion.backend.modules.ai.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendRequest {

    @NotBlank(message = "Dịp sử dụng không được để trống")
    private String occasion;

    @NotBlank(message = "Phong cách không được để trống")
    private String style;

    private String preferredColor;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private GenderTarget genderTarget;
}
