package com.fashion.backend.modules.product.controller;

import com.fashion.backend.common.api.ApiResponse;
import com.fashion.backend.common.api.PageResponse;
import com.fashion.backend.modules.product.dto.request.ProductFilterRequest;
import com.fashion.backend.modules.product.dto.response.ProductDetailResponse;
import com.fashion.backend.modules.product.dto.response.ProductSummaryResponse;
import com.fashion.backend.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            @ModelAttribute ProductFilterRequest filterRequest) {
        PageResponse<ProductSummaryResponse> response = productService.getProducts(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable Long id) {
        ProductDetailResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", response));
    }
}
