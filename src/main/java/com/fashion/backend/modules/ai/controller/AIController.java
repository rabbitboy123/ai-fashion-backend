package com.fashion.backend.modules.ai.controller;

import com.fashion.backend.common.api.ApiResponse;
import com.fashion.backend.modules.ai.dto.request.AIRecommendRequest;
import com.fashion.backend.modules.ai.dto.response.AIRecommendResponse;
import com.fashion.backend.modules.ai.service.AIService;
import com.fashion.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    /**
     * Tư vấn phối đồ thời trang AI Haute Couture
     * Cho phép cả khách vãng lai và người dùng đã đăng nhập sử dụng.
     */
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<AIRecommendResponse>> recommendOutfit(
            @Valid @RequestBody AIRecommendRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = (userPrincipal != null) ? userPrincipal.getId() : null;
        AIRecommendResponse response = aiService.recommendOutfit(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Tư vấn phối đồ AI thành công", response));
    }

    /**
     * Lấy lịch sử tư vấn phối đồ của người dùng hiện tại (yêu cầu đăng nhập)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AIRecommendResponse>>> getHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<AIRecommendResponse> history = aiService.getRecommendationHistory(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử tư vấn AI thành công", history));
    }
}
