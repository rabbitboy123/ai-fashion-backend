package com.fashion.backend.modules.ai.engine;

import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import com.fashion.backend.modules.ai.dto.request.AIRecommendRequest;
import com.fashion.backend.modules.ai.dto.response.AIProductItemResponse;
import com.fashion.backend.modules.ai.dto.response.AIVariantItemResponse;
import com.fashion.backend.modules.product.entity.Product;
import com.fashion.backend.modules.product.entity.ProductVariant;
import com.fashion.backend.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final ProductRepository productRepository;

    /**
     * Chấm điểm và xếp hạng sản phẩm thật từ Database theo 5 tiêu chí chuẩn:
     * - Phong cách (Style Match): Max 30đ
     * - Gam màu (Color Match): Max 20đ
     * - Ngân sách (Budget Match): Max 20đ
     * - Dịp sử dụng (Occasion Match): Max 15đ
     * - Đối tượng (Gender Target Match): Max 15đ
     * Tổng điểm tối đa: 100 điểm.
     */
    public List<AIProductItemResponse> scoreAndRank(AIRecommendRequest request, int topN) {
        List<Product> allProducts = productRepository.findAll();

        // 1. Lọc sản phẩm ACTIVE và có biến thể còn hàng
        List<Product> availableProducts = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .filter(p -> p.getVariants() != null && p.getVariants().stream().anyMatch(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0))
                .collect(Collectors.toList());

        if (availableProducts.isEmpty()) {
            log.warn("Không tìm thấy sản phẩm khả dụng trong Database để gợi ý.");
            return Collections.emptyList();
        }

        // 2. Chấm điểm từng sản phẩm
        List<ScoredProduct> scoredList = new ArrayList<>();
        for (Product product : availableProducts) {
            ScoredProduct scored = evaluateProduct(product, request);
            scoredList.add(scored);
        }

        // 3. Sắp xếp giảm dần theo điểm số
        scoredList.sort((a, b) -> Integer.compare(b.totalScore, a.totalScore));

        // 4. Lựa chọn Top N ưu tiên sự đa dạng danh mục (tạo bộ trang phục phối hợp hoàn chỉnh)
        List<ScoredProduct> selected = new ArrayList<>();
        Set<Long> categoryIdsUsed = new HashSet<>();

        // Lượt 1: Chọn sản phẩm điểm cao nhất từ các danh mục khác nhau
        for (ScoredProduct sp : scoredList) {
            Long catId = (sp.product.getCategory() != null) ? sp.product.getCategory().getId() : null;
            if (catId != null && !categoryIdsUsed.contains(catId)) {
                selected.add(sp);
                categoryIdsUsed.add(catId);
                if (selected.size() == topN) break;
            }
        }

        // Lượt 2: Nếu chưa đủ topN danh mục khác nhau, lấy tiếp các sản phẩm có điểm cao nhất còn lại
        if (selected.size() < topN) {
            for (ScoredProduct sp : scoredList) {
                if (!selected.contains(sp)) {
                    selected.add(sp);
                    if (selected.size() == topN) break;
                }
            }
        }

        // 5. Chuyển đổi sang AIProductItemResponse
        return selected.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    private ScoredProduct evaluateProduct(Product p, AIRecommendRequest req) {
        int styleScore = calculateStyleScore(p.getStyle(), req.getStyle());
        int colorScore = calculateColorScore(p.getVariants(), req.getPreferredColor());
        int budgetScore = calculateBudgetScore(p.getPrice(), req.getBudgetMin(), req.getBudgetMax());
        int occasionScore = calculateOccasionScore(p.getOccasion(), req.getOccasion());
        int targetScore = calculateTargetScore(p.getGenderTarget(), req.getGenderTarget());

        int total = styleScore + colorScore + budgetScore + occasionScore + targetScore;
        total = Math.min(100, Math.max(0, total));

        String matchReason = buildMatchReason(styleScore, colorScore, budgetScore, occasionScore, targetScore, p, req);

        return new ScoredProduct(p, total, matchReason);
    }

    private int calculateStyleScore(String productStyle, String requestedStyle) {
        if (requestedStyle == null || requestedStyle.isBlank()) return 20;
        if (productStyle == null) return 10;

        String ps = productStyle.toLowerCase();
        String rs = requestedStyle.toLowerCase();

        if (ps.contains(rs) || rs.contains(ps)) return 30;
        if (ps.contains("luxury") && (rs.contains("sang trọng") || rs.contains("bespoke"))) return 28;
        if (ps.contains("tối giản") && rs.contains("minimalist")) return 28;
        if (ps.contains("cổ điển") && rs.contains("classic")) return 28;
        if (ps.contains("casual") && rs.contains("smart")) return 25;

        return 12;
    }

    private int calculateColorScore(List<ProductVariant> variants, String preferredColor) {
        if (preferredColor == null || preferredColor.isBlank() || preferredColor.equalsIgnoreCase("all")) return 20;
        if (variants == null || variants.isEmpty()) return 10;

        String target = preferredColor.toLowerCase();
        for (ProductVariant v : variants) {
            if (v.getColor() != null && v.getColor().getName() != null) {
                String colorName = v.getColor().getName().toLowerCase();
                if (colorName.contains(target) || target.contains(colorName)) {
                    return 20;
                }
                // Đồng nghĩa màu sắc thời trang
                if ((target.contains("đen") || target.contains("charcoal")) && colorName.contains("đen")) return 20;
                if ((target.contains("trắng") || target.contains("cream")) && (colorName.contains("trắng") || colorName.contains("kem"))) return 20;
                if ((target.contains("vàng") || target.contains("gold")) && (colorName.contains("vàng") || colorName.contains("cát"))) return 20;
                if ((target.contains("xanh") || target.contains("teal")) && (colorName.contains("xanh") || colorName.contains("rêu"))) return 20;
                if ((target.contains("đỏ") || target.contains("plum")) && (colorName.contains("đỏ") || colorName.contains("mận"))) return 20;
            }
        }
        return 8;
    }

    private int calculateBudgetScore(BigDecimal price, BigDecimal minBudget, BigDecimal maxBudget) {
        if (price == null) return 10;
        if (minBudget == null && maxBudget == null) return 20;

        double p = price.doubleValue();
        double min = (minBudget != null) ? minBudget.doubleValue() : 0.0;
        double max = (maxBudget != null) ? maxBudget.doubleValue() : Double.MAX_VALUE;

        if (p >= min && p <= max) {
            return 20;
        }

        if (maxBudget != null && p > max) {
            double ratio = (p - max) / max;
            if (ratio <= 0.25) return 12;
            if (ratio <= 0.50) return 6;
            return 2;
        }

        if (minBudget != null && p < min) {
            return 14;
        }

        return 10;
    }

    private int calculateOccasionScore(String productOccasion, String requestedOccasion) {
        if (requestedOccasion == null || requestedOccasion.isBlank()) return 15;
        if (productOccasion == null) return 8;

        String po = productOccasion.toLowerCase();
        String ro = requestedOccasion.toLowerCase();

        if (po.contains(ro) || ro.contains(po)) return 15;
        if (po.contains("tiệc") && (ro.contains("party") || ro.contains("dạ hội"))) return 15;
        if (po.contains("công sở") && (ro.contains("work") || ro.contains("đi làm"))) return 15;
        if (po.contains("dạo phố") && (ro.contains("casual") || ro.contains("hẹn hò"))) return 12;

        return 6;
    }

    private int calculateTargetScore(GenderTarget productTarget, GenderTarget requestedTarget) {
        if (requestedTarget == null || requestedTarget == GenderTarget.UNISEX) return 15;
        if (productTarget == null || productTarget == GenderTarget.UNISEX) return 15;
        if (productTarget == requestedTarget) return 15;
        return 2;
    }

    private String buildMatchReason(int style, int color, int budget, int occasion, int target, Product p, AIRecommendRequest req) {
        List<String> highlights = new ArrayList<>();
        if (style >= 25) highlights.add("Phong cách " + p.getStyle());
        if (occasion >= 12) highlights.add("Đúng dịp " + p.getOccasion());
        if (color >= 18 && req.getPreferredColor() != null) highlights.add("Tone màu chuẩn");
        if (budget >= 18) highlights.add("Vừa vặn ngân sách");

        if (highlights.isEmpty()) {
            return "Hài hòa với cấu trúc tổng thể bộ trang phục";
        }
        return String.join(" • ", highlights);
    }

    private AIProductItemResponse mapToItemResponse(ScoredProduct sp) {
        Product p = sp.product;
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
                .category((p.getCategory() != null) ? p.getCategory().getName() : "Haute Couture")
                .brand((p.getBrand() != null) ? p.getBrand() : "Bespoke Atelier")
                .material(p.getMaterial())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .style(p.getStyle())
                .occasion(p.getOccasion())
                .score(sp.totalScore)
                .matchReason(sp.matchReason)
                .availableVariants(variants)
                .build();
    }

    private static class ScoredProduct {
        final Product product;
        final int totalScore;
        final String matchReason;

        ScoredProduct(Product product, int totalScore, String matchReason) {
            this.product = product;
            this.totalScore = totalScore;
            this.matchReason = matchReason;
        }
    }
}
