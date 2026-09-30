package com.fashion.backend.modules.ai.service;

import com.fashion.backend.modules.ai.client.GeminiClient;
import com.fashion.backend.modules.ai.dto.request.AIRecommendRequest;
import com.fashion.backend.modules.ai.dto.response.AIProductItemResponse;
import com.fashion.backend.modules.ai.dto.response.AIRecommendResponse;
import com.fashion.backend.modules.ai.dto.response.AIVariantItemResponse;
import com.fashion.backend.modules.ai.engine.RecommendationEngine;
import com.fashion.backend.modules.ai.entity.AIRecommendation;
import com.fashion.backend.modules.ai.repository.AIRecommendationRepository;
import com.fashion.backend.modules.product.entity.Product;
import com.fashion.backend.modules.product.repository.ProductRepository;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final RecommendationEngine recommendationEngine;
    private final GeminiClient geminiClient;
    private final AIRecommendationRepository aiRecommendationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public AIRecommendResponse recommendOutfit(AIRecommendRequest request, Long userId) {
        log.info("Bắt đầu xử lý tư vấn phong cách AI cho dịp: {}, phong cách: {}", request.getOccasion(), request.getStyle());

        // 1. Chấm điểm và lọc Top 3 sản phẩm thật từ Database Neon
        List<AIProductItemResponse> topProducts = recommendationEngine.scoreAndRank(request, 3);

        if (topProducts.isEmpty()) {
            return AIRecommendResponse.builder()
                    .advice("Rất tiếc hiện tại Atelier chưa có sản phẩm phù hợp với tiêu chí đã chọn.")
                    .scoreAvg(0)
                    .items(Collections.emptyList())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // 2. Gọi Google Gemini API để tạo lời bình thời trang Haute Couture (kèm Fallback)
        String stylingAdvice = geminiClient.generateStylingAdvice(request, topProducts);

        // 3. Tính điểm trung bình của bộ trang phục
        double avg = topProducts.stream().mapToInt(AIProductItemResponse::getScore).average().orElse(90.0);
        int scoreAvg = (int) Math.round(avg);

        Long savedId = null;

        // 4. Nếu người dùng đã đăng nhập, lưu lại lịch sử tư vấn vào Database Neon
        if (userId != null) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                String productIdsStr = topProducts.stream()
                        .map(p -> String.valueOf(p.getId()))
                        .collect(Collectors.joining(","));

                AIRecommendation rec = AIRecommendation.builder()
                        .user(user)
                        .occasion(request.getOccasion())
                        .style(request.getStyle())
                        .preferredColor(request.getPreferredColor())
                        .budgetMin(request.getBudgetMin())
                        .budgetMax(request.getBudgetMax())
                        .recommendedProductIds(productIdsStr)
                        .aiExplanation(stylingAdvice)
                        .build();

                AIRecommendation saved = aiRecommendationRepository.save(rec);
                savedId = saved.getId();
            } catch (Exception e) {
                log.warn("Không thể lưu lịch sử AI recommendation: {}", e.getMessage());
            }
        }

        return AIRecommendResponse.builder()
                .recommendationId(savedId)
                .advice(stylingAdvice)
                .scoreAvg(scoreAvg)
                .items(topProducts)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIRecommendResponse> getRecommendationHistory(Long userId) {
        List<AIRecommendation> historyList = aiRecommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return historyList.stream().map(rec -> {
            List<AIProductItemResponse> items = new ArrayList<>();
            if (rec.getRecommendedProductIds() != null && !rec.getRecommendedProductIds().isBlank()) {
                String[] ids = rec.getRecommendedProductIds().split(",");
                for (String idStr : ids) {
                    try {
                        Long pid = Long.parseLong(idStr.trim());
                        productRepository.findById(pid).ifPresent(p -> items.add(mapProductToItemResponse(p)));
                    } catch (NumberFormatException ignored) {}
                }
            }

            return AIRecommendResponse.builder()
                    .recommendationId(rec.getId())
                    .advice(rec.getAiExplanation())
                    .scoreAvg(95)
                    .items(items)
                    .createdAt(rec.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private AIProductItemResponse mapProductToItemResponse(Product p) {
        List<AIVariantItemResponse> variants = (p.getVariants() == null) ? Collections.emptyList() :
                p.getVariants().stream()
                        .map(v -> AIVariantItemResponse.builder()
                                .id(v.getId())
                                .size((v.getSize() != null) ? v.getSize().getName() : "")
                                .sizeName((v.getSize() != null) ? v.getSize().getName() : "")
                                .color((v.getColor() != null) ? v.getColor().getName() : "")
                                .colorName((v.getColor() != null) ? v.getColor().getName() : "")
                                .colorHex((v.getColor() != null) ? v.getColor().getHexCode() : "#121212")
                                .stockQuantity(v.getStockQuantity())
                                .additionalPrice(v.getAdditionalPrice())
                                .build())
                        .collect(Collectors.toList());

        return AIProductItemResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .category((p.getCategory() != null) ? p.getCategory().getName() : "")
                .brand(p.getBrand())
                .material(p.getMaterial())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .style(p.getStyle())
                .occasion(p.getOccasion())
                .score(95)
                .matchReason("Sản phẩm thuộc lịch sử tư vấn Haute Couture")
                .availableVariants(variants)
                .build();
    }
}
