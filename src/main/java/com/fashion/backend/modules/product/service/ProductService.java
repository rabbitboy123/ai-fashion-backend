package com.fashion.backend.modules.product.service;

import com.fashion.backend.common.api.PageResponse;
import com.fashion.backend.modules.product.dto.request.AdminCreateProductRequest;
import com.fashion.backend.modules.product.dto.request.AdminCreateVariantRequest;
import com.fashion.backend.modules.product.dto.request.AdminUpdateProductRequest;
import com.fashion.backend.modules.product.dto.request.ProductFilterRequest;
import com.fashion.backend.modules.product.dto.response.ProductDetailResponse;
import com.fashion.backend.modules.product.dto.response.ProductSummaryResponse;
import com.fashion.backend.modules.product.dto.response.ProductVariantResponse;
import java.util.List;

public interface ProductService {
    PageResponse<ProductSummaryResponse> getProducts(ProductFilterRequest request);

    ProductDetailResponse getProductById(Long id);

    List<ProductDetailResponse> getAllProductsForAdmin();

    ProductDetailResponse createProduct(AdminCreateProductRequest request);

    ProductDetailResponse updateProduct(Long id, AdminUpdateProductRequest request);

    void deleteProduct(Long id);

    ProductVariantResponse addVariant(Long productId, AdminCreateVariantRequest request);

    void deleteVariant(Long variantId);

    ProductVariantResponse updateVariantStock(Long variantId, Integer newStock);
}