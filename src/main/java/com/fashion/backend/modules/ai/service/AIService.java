package com.fashion.backend.modules.ai.service;

import com.fashion.backend.modules.ai.dto.request.AIRecommendRequest;
import com.fashion.backend.modules.ai.dto.response.AIRecommendResponse;

import java.util.List;

public interface AIService {
    AIRecommendResponse recommendOutfit(AIRecommendRequest request, Long userId);
    List<AIRecommendResponse> getRecommendationHistory(Long userId);
}
