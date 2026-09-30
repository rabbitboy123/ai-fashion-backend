package com.fashion.backend.modules.product.controller;

import com.fashion.backend.common.api.ApiResponse;
import com.fashion.backend.modules.product.dto.request.AdminCreateProductRequest;
import com.fashion.backend.modules.product.dto.request.AdminCreateVariantRequest;
import com.fashion.backend.modules.product.dto.request.AdminUpdateProductRequest;
import com.fashion.backend.modules.product.dto.request.AdminUpdateStockRequest;
import com.fashion.backend.modules.product.dto.response.ProductDetailResponse;
import com.fashion.backend.modules.product.dto.response.ProductVariantResponse;
import com.fashion.backend.modules.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDetailResponse>>> getAllProducts() {
        List<ProductDetailResponse> response = productService.getAllProductsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ danh sách sản phẩm quản trị thành công", response));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody AdminCreateProductRequest request) {
        ProductDetailResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm tác phẩm mới thành công", response));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateProductRequest request) {
        ProductDetailResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin tác phẩm thành công", response));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tác phẩm thành công", null));
    }

    @PostMapping("/products/{id}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable Long id,
            @Valid @RequestBody AdminCreateVariantRequest request) {
        ProductVariantResponse response = productService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm biến thể mới thành công", response));
    }

    @DeleteMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long id) {
        productService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa biến thể thành công", null));
    }

    @PutMapping("/variants/{id}/stock")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariantStock(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateStockRequest request) {
        ProductVariantResponse response = productService.updateVariantStock(id, request.getStockQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tồn kho thành công", response));
    }
}
