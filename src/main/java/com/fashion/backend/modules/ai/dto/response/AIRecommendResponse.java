package com.fashion.backend.modules.ai.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendResponse {
    private Long recommendationId;
    private String advice;
    private Integer scoreAvg;
    private List<AIProductItemResponse> items;
    private LocalDateTime createdAt;
}
