package com.fashion.backend.modules.ai.dto.request;

import com.fashion.backend.common.constant.GenderTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendRequest {

    @NotBlank(message = "Dịp sử dụng không được để trống")
    @Size(max = 100, message = "Dịp sử dụng quá dài")
    private String occasion;

    @NotBlank(message = "Phong cách không được để trống")
    @Size(max = 100, message = "Phong cách quá dài")
    private String style;

    @Size(max = 100, message = "Màu sắc quá dài")
    private String preferredColor;

    @DecimalMin(value = "0.0", message = "Ngân sách tối thiểu không được âm")
    @Digits(integer = 10, fraction = 2, message = "Ngân sách tối thiểu không đúng định dạng")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.0", message = "Ngân sách tối đa không được âm")
    @Digits(integer = 10, fraction = 2, message = "Ngân sách tối đa không đúng định dạng")
    private BigDecimal budgetMax;

    private GenderTarget genderTarget;

    @AssertTrue(message = "Ngân sách tối thiểu không được lớn hơn ngân sách tối đa")
    public boolean isBudgetRangeValid() {
        return budgetMin == null || budgetMax == null || budgetMin.compareTo(budgetMax) <= 0;
    }
}
