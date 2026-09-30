package com.fashion.backend.modules.product.service;

import com.fashion.backend.common.api.PageResponse;
import com.fashion.backend.common.exception.ResourceNotFoundException;
import com.fashion.backend.modules.product.dto.request.ProductFilterRequest;
import com.fashion.backend.modules.product.dto.response.CategoryResponse;
import com.fashion.backend.modules.product.dto.response.ProductDetailResponse;
import com.fashion.backend.modules.product.dto.response.ProductSummaryResponse;
import com.fashion.backend.modules.product.dto.response.ProductVariantResponse;
import com.fashion.backend.modules.product.entity.Product;
import com.fashion.backend.modules.product.entity.ProductVariant;
import com.fashion.backend.modules.product.repository.ProductRepository;
import com.fashion.backend.modules.product.specification.ProductSpecification;
import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import com.fashion.backend.modules.product.dto.request.AdminCreateProductRequest;
import com.fashion.backend.modules.product.dto.request.AdminCreateVariantRequest;
import com.fashion.backend.modules.product.dto.request.AdminUpdateProductRequest;
import com.fashion.backend.modules.product.entity.Category;
import com.fashion.backend.modules.product.entity.Color;
import com.fashion.backend.modules.product.entity.Size;
import com.fashion.backend.modules.product.repository.CategoryRepository;
import com.fashion.backend.modules.product.repository.ColorRepository;
import com.fashion.backend.modules.product.repository.ProductVariantRepository;
import com.fashion.backend.modules.product.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final ProductVariantRepository productVariantRepository;

    public PageResponse<ProductSummaryResponse> getProducts(ProductFilterRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (request != null && request.getSortBy() != null) {
            switch (request.getSortBy().toLowerCase()) {
                case "priceasc":
                    sort = Sort.by(Sort.Direction.ASC, "price");
                    break;
                case "pricedesc":
                    sort = Sort.by(Sort.Direction.DESC, "price");
                    break;
                case "newest":
                default:
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
            }
        }

        int pageIndex = (request != null && request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        int pageSize = (request != null && request.getSize() != null && request.getSize() > 0) ? request.getSize() : 12;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
        Page<Product> productPage = productRepository.findAll(ProductSpecification.filter(request), pageable);

        Page<ProductSummaryResponse> responsePage = productPage.map(this::mapToSummaryResponse);
        return PageResponse.from(responsePage);
    }

    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        return mapToDetailResponse(product);
    }

    private ProductSummaryResponse mapToSummaryResponse(Product product) {
        List<String> colors = product.getVariants().stream()
                .map(v -> v.getColor().getName())
                .distinct()
                .collect(Collectors.toList());

        List<String> sizes = product.getVariants().stream()
                .map(v -> v.getSize().getName())
                .distinct()
                .collect(Collectors.toList());

        int totalStock = product.getVariants().stream()
                .mapToInt(ProductVariant::getStockQuantity)
                .sum();

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brand(product.getBrand())
                .style(product.getStyle())
                .occasion(product.getOccasion())
                .genderTarget(product.getGenderTarget())
                .availableColors(colors)
                .availableSizes(sizes)
                .totalStock(totalStock)
                .build();
    }

    private ProductDetailResponse mapToDetailResponse(Product product) {
        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(product.getCategory().getId())
                .name(product.getCategory().getName())
                .description(product.getCategory().getDescription())
                .build();

        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .map(variant -> {
                    BigDecimal additional = variant.getAdditionalPrice() != null ? variant.getAdditionalPrice() : BigDecimal.ZERO;
                    BigDecimal finalPrice = product.getPrice().add(additional);

                    return ProductVariantResponse.builder()
                            .id(variant.getId())
                            .sizeId(variant.getSize().getId())
                            .sizeName(variant.getSize().getName())
                            .colorId(variant.getColor().getId())
                            .colorName(variant.getColor().getName())
                            .colorHex(variant.getColor().getHexCode())
                            .sku(variant.getSku())
                            .stockQuantity(variant.getStockQuantity())
                            .additionalPrice(additional)
                            .finalPrice(finalPrice)
                            .imageUrl(variant.getImageUrl() != null ? variant.getImageUrl() : product.getImageUrl())
                            .build();
                })
                .collect(Collectors.toList());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .material(product.getMaterial())
                .genderTarget(product.getGenderTarget())
                .style(product.getStyle())
                .occasion(product.getOccasion())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .category(categoryResponse)
                .variants(variantResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public List<ProductDetailResponse> getAllProductsForAdmin() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::mapToDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDetailResponse createProduct(AdminCreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .category(category)
                .price(request.getPrice())
                .brand(request.getBrand() != null && !request.getBrand().isBlank() ? request.getBrand() : "Bespoke Atelier")
                .material(request.getMaterial())
                .genderTarget(request.getGenderTarget() != null ? request.getGenderTarget() : GenderTarget.UNISEX)
                .style(request.getStyle() != null && !request.getStyle().isBlank() ? request.getStyle() : "CLASSIC")
                .occasion(request.getOccasion() != null && !request.getOccasion().isBlank() ? request.getOccasion() : "DAILY")
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);

        // Tự động khởi tạo 1 biến thể mặc định để sản phẩm mới lập tức sẵn sàng kinh doanh và hiển thị tồn kho
        Size defaultSize = sizeRepository.findByName("M")
                .orElseGet(() -> sizeRepository.save(Size.builder().name("M").build()));
        Color defaultColor = colorRepository.findByName("Đen Onyx")
                .orElseGet(() -> colorRepository.save(Color.builder().name("Đen Onyx").hexCode("#121212").build()));

        ProductVariant defaultVariant = ProductVariant.builder()
                .product(saved)
                .size(defaultSize)
                .color(defaultColor)
                .sku("SKU-" + saved.getId() + "-M-" + (System.currentTimeMillis() % 10000))
                .stockQuantity(15)
                .additionalPrice(BigDecimal.ZERO)
                .imageUrl(saved.getImageUrl())
                .build();

        productVariantRepository.save(defaultVariant);
        saved.setVariants(new ArrayList<>(List.of(defaultVariant)));

        return mapToDetailResponse(saved);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long id, AdminUpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getMaterial() != null) product.setMaterial(request.getMaterial());
        if (request.getGenderTarget() != null) product.setGenderTarget(request.getGenderTarget());
        if (request.getStyle() != null) product.setStyle(request.getStyle());
        if (request.getOccasion() != null) product.setOccasion(request.getOccasion());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getStatus() != null) product.setStatus(request.getStatus());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        return mapToDetailResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    @Transactional
    public ProductVariantResponse addVariant(Long productId, AdminCreateVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        // Resolve Size
        Size size;
        if (request.getSizeId() != null) {
            size = sizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kích thước với ID: " + request.getSizeId()));
        } else if (request.getSizeName() != null && !request.getSizeName().isBlank()) {
            String sName = request.getSizeName().trim().toUpperCase();
            size = sizeRepository.findByName(sName)
                    .orElseGet(() -> sizeRepository.save(Size.builder().name(sName).build()));
        } else {
            size = sizeRepository.findByName("M")
                    .orElseGet(() -> sizeRepository.save(Size.builder().name("M").build()));
        }

        // Resolve Color
        Color color;
        if (request.getColorId() != null) {
            color = colorRepository.findById(request.getColorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc với ID: " + request.getColorId()));
        } else if (request.getColorName() != null && !request.getColorName().isBlank()) {
            String cName = request.getColorName().trim();
            color = colorRepository.findByName(cName)
                    .orElseGet(() -> colorRepository.save(Color.builder()
                            .name(cName)
                            .hexCode(request.getColorHex() != null ? request.getColorHex() : "#121212")
                            .build()));
        } else {
            color = colorRepository.findByName("Đen Onyx")
                    .orElseGet(() -> colorRepository.save(Color.builder().name("Đen Onyx").hexCode("#121212").build()));
        }

        String sku = (request.getSku() != null && !request.getSku().isBlank())
                ? request.getSku()
                : "SKU-" + productId + "-" + size.getName() + "-" + (System.currentTimeMillis() % 10000);

        ProductVariant variant = productVariantRepository.findByProductIdAndSizeIdAndColorId(productId, size.getId(), color.getId())
                .orElse(null);

        if (variant != null) {
            variant.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 10);
            if (request.getAdditionalPrice() != null) variant.setAdditionalPrice(request.getAdditionalPrice());
            if (request.getImageUrl() != null) variant.setImageUrl(request.getImageUrl());
        } else {
            variant = ProductVariant.builder()
                    .product(product)
                    .size(size)
                    .color(color)
                    .sku(sku)
                    .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 10)
                    .additionalPrice(request.getAdditionalPrice() != null ? request.getAdditionalPrice() : BigDecimal.ZERO)
                    .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : product.getImageUrl())
                    .build();
            product.getVariants().add(variant);
        }

        ProductVariant saved = productVariantRepository.save(variant);
        return mapToVariantResponse(saved);
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId));
        productVariantRepository.delete(variant);
    }

    @Transactional
    public ProductVariantResponse updateVariantStock(Long variantId, Integer newStock) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId));
        variant.setStockQuantity(newStock != null && newStock >= 0 ? newStock : 0);
        ProductVariant saved = productVariantRepository.save(variant);
        return mapToVariantResponse(saved);
    }

    public ProductVariantResponse mapToVariantResponse(ProductVariant variant) {
        BigDecimal additional = variant.getAdditionalPrice() != null ? variant.getAdditionalPrice() : BigDecimal.ZERO;
        BigDecimal finalPrice = variant.getProduct().getPrice().add(additional);

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sizeId(variant.getSize().getId())
                .sizeName(variant.getSize().getName())
                .colorId(variant.getColor().getId())
                .colorName(variant.getColor().getName())
                .colorHex(variant.getColor().getHexCode())
                .sku(variant.getSku())
                .stockQuantity(variant.getStockQuantity())
                .additionalPrice(additional)
                .finalPrice(finalPrice)
                .imageUrl(variant.getImageUrl() != null ? variant.getImageUrl() : variant.getProduct().getImageUrl())
                .build();
    }
}

