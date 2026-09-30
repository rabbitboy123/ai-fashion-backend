package com.fashion.backend.modules.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.backend.modules.ai.dto.request.AIRecommendRequest;
import com.fashion.backend.modules.ai.dto.response.AIProductItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    public GeminiClient(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(12))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi yêu cầu phân tích phối đồ Haute Couture tới Google Gemini API.
     * TUÂN THỦ NGHIÊM NGẶT GEMINI.MD:
     * - Chỉ truyền dữ liệu sản phẩm THẬT được chọn từ Database.
     * - Gemini tuyệt đối KHÔNG tự sáng tác sản phẩm, giá hay tồn kho.
     * - Có Fallback thông minh nếu mất kết nối hoặc hết quota.
     */
    public String generateStylingAdvice(AIRecommendRequest request, List<AIProductItemResponse> products) {
        if (products == null || products.isEmpty()) {
            return "Hiện tại Atelier chưa tìm thấy tác phẩm phù hợp trọn vẹn với tiêu chí của bạn. Xin vui lòng điều chỉnh khoảng ngân sách hoặc phong cách.";
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Gemini API Key chưa được thiết lập. Kích hoạt Haute Couture Deterministic Fallback.");
            return buildDeterministicFallback(request, products);
        }

        try {
            String prompt = buildPrompt(request, products);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey.trim();

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));
            Map<String, Object> contents = Map.of("contents", List.of(parts));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(contents, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        String generatedText = textNode.asText().trim();
                        log.info("Gemini AI tư vấn thành công: {} ký tự.", generatedText.length());
                        return generatedText;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Gọi Gemini API không thành công ({}). Chuyển sang Haute Couture Fallback an toàn.", e.getMessage());
        }

        return buildDeterministicFallback(request, products);
    }

    private String buildPrompt(AIRecommendRequest req, List<AIProductItemResponse> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là Chuyên gia Giám tuyển Phong cách Thời trang Cao cấp (Senior Haute Couture Stylist) tại Bespoke Atelier.\n");
        sb.append("Nhiệm vụ của bạn: Viết lời tư vấn và bình giải nghệ thuật phối đồ (từ 2 đến 3 câu tinh tế, sang trọng, trang nhã bằng tiếng Việt) ");
        sb.append("dựa trên chính xác 3 tác phẩm THỰC TẾ sau đây của cửa hàng.\n\n");

        sb.append("Thông tin khách hàng mong muốn:\n");
        sb.append("- Dịp sử dụng: ").append(req.getOccasion()).append("\n");
        sb.append("- Phong cách chủ đạo: ").append(req.getStyle()).append("\n");
        if (req.getPreferredColor() != null) sb.append("- Tone màu ưu tiên: ").append(req.getPreferredColor()).append("\n");
        if (req.getBudgetMax() != null) sb.append("- Ngân sách: Khoảng ").append(req.getBudgetMax()).append(" VNĐ\n");

        sb.append("\nDanh sách 3 tác phẩm THỰC TẾ được gợi ý:\n");
        for (int i = 0; i < products.size(); i++) {
            AIProductItemResponse p = products.get(i);
            sb.append(i + 1).append(". ")
              .append(p.getName()).append(" (Chất liệu: ").append(p.getMaterial() != null ? p.getMaterial() : "Cao cấp")
              .append(", Giá: ").append(p.getPrice()).append(" VNĐ, Thương hiệu: ").append(p.getBrand()).append(")\n");
        }

        sb.append("\nQuy tắc bắt buộc:\n");
        sb.append("1. Chỉ nhắc đến tên các tác phẩm có trong danh sách trên, tuyệt đối không bịa thêm sản phẩm mới hay giá cả.\n");
        sb.append("2. Giải thích lý do vì sao sự kết hợp của 3 món đồ này tạo nên cấu trúc hình thể thanh thoát, hài hòa về chất liệu và tôn vinh thần thái của người mặc trong dịp ").append(req.getOccasion()).append(".\n");
        sb.append("3. Giọng văn lịch thiệp, quý phái của thời trang Bespoke xa xỉ.");

        return sb.toString();
    }

    private String buildDeterministicFallback(AIRecommendRequest req, List<AIProductItemResponse> products) {
        String item1 = products.get(0).getName();
        String item2 = products.size() > 1 ? products.get(1).getName() : "Phụ kiện đi kèm";
        String item3 = products.size() > 2 ? products.get(2).getName() : "Chi tiết điểm xuyết";

        String occasion = (req.getOccasion() != null) ? req.getOccasion() : "Sự kiện đặc biệt";
        String style = (req.getStyle() != null) ? req.getStyle() : "Bespoke Luxury";

        return String.format(
                "Dành cho dịp %s theo phong cách %s, sự phối hợp đồng điệu giữa %s và %s tạo nên một kết cấu hình thể thanh thoát và đầy cuốn hút. Điểm xuyết cùng %s sẽ tôn vinh trọn vẹn nét trang nhã, sang trọng mà vẫn giữ được sự tinh tế chuẩn mực của phong cách Haute Couture.",
                occasion, style, item1, item2, item3
        );
    }
}
