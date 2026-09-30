package com.fashion.backend.modules.ai.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIProductItemResponse {
    private Long id;
    private String name;
    private String category;
    private String brand;
    private String material;
    private BigDecimal price;
    private String imageUrl;
    private String style;
    private String occasion;
    private Integer score;
    private String matchReason;
    private List<AIVariantItemResponse> availableVariants;
}
