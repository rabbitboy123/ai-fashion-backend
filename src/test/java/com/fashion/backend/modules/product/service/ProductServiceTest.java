package com.fashion.backend.modules.product.service;

import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import com.fashion.backend.modules.product.dto.request.AdminCreateProductRequest;
import com.fashion.backend.modules.product.dto.request.AdminCreateVariantRequest;
import com.fashion.backend.modules.product.dto.response.ProductDetailResponse;
import com.fashion.backend.modules.product.dto.response.ProductVariantResponse;
import com.fashion.backend.modules.product.entity.Category;
import com.fashion.backend.modules.product.entity.Color;
import com.fashion.backend.modules.product.entity.Product;
import com.fashion.backend.modules.product.entity.ProductVariant;
import com.fashion.backend.modules.product.entity.Size;
import com.fashion.backend.modules.product.repository.CategoryRepository;
import com.fashion.backend.modules.product.repository.ColorRepository;
import com.fashion.backend.modules.product.repository.ProductRepository;
import com.fashion.backend.modules.product.repository.ProductVariantRepository;
import com.fashion.backend.modules.product.repository.SizeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SizeRepository sizeRepository;

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category sampleCategory;
    private Size sampleSize;
    private Color sampleColor;
    private Product sampleProduct;
    private ProductVariant sampleVariant;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder()
                .id(1L)
                .name("Đầm Dạ Hội")
                .description("Haute Couture Dresses")
                .build();

        sampleSize = Size.builder()
                .id(1L)
                .name("M")
                .build();

        sampleColor = Color.builder()
                .id(1L)
                .name("Đen Onyx")
                .hexCode("#121212")
                .build();

        sampleProduct = Product.builder()
                .id(10L)
                .name("Đầm Lụa Dạ Hội Thượng Hạng")
                .category(sampleCategory)
                .price(new BigDecimal("15000000.00"))
                .brand("Bespoke Atelier")
                .material("Lụa Tơ Tằm")
                .genderTarget(GenderTarget.WOMEN)
                .style("Sang Trọng")
                .occasion("Dạ Tiệc")
                .imageUrl("https://example.com/dress.jpg")
                .status(ProductStatus.ACTIVE)
                .variants(new ArrayList<>())
                .build();

        sampleVariant = ProductVariant.builder()
                .id(100L)
                .product(sampleProduct)
                .size(sampleSize)
                .color(sampleColor)
                .sku("SKU-10-M-1234")
                .stockQuantity(15)
                .additionalPrice(BigDecimal.ZERO)
                .imageUrl("https://example.com/dress.jpg")
                .build();

        sampleProduct.getVariants().add(sampleVariant);
    }

    @Test
    @DisplayName("getAllProductsForAdmin trả về đầy đủ sản phẩm và biến thể thật")
    void getAllProductsForAdmin_ShouldReturnAllProductsWithVariants() {
        when(productRepository.findAll(any(Sort.class))).thenReturn(List.of(sampleProduct));

        List<ProductDetailResponse> results = productService.getAllProductsForAdmin();

        assertNotNull(results);
        assertEquals(1, results.size());
        ProductDetailResponse dto = results.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("Đầm Lụa Dạ Hội Thượng Hạng", dto.getName());
        assertEquals(1, dto.getVariants().size());
        assertEquals("M", dto.getVariants().get(0).getSizeName());
        assertEquals("Đen Onyx", dto.getVariants().get(0).getColorName());
        assertEquals(15, dto.getVariants().get(0).getStockQuantity());
    }

    @Test
    @DisplayName("createProduct tự động khởi tạo biến thể mặc định sẵn sàng kinh doanh")
    void createProduct_ShouldCreateProductAndDefaultVariant() {
        AdminCreateProductRequest request = AdminCreateProductRequest.builder()
                .name("Áo Dài Di Sản Thêu Tay")
                .categoryId(1L)
                .price(new BigDecimal("12000000.00"))
                .brand("Bespoke Atelier")
                .material("Lụa Hà Đông")
                .genderTarget(GenderTarget.WOMEN)
                .style("Truyền Thống")
                .occasion("Lễ Hội")
                .imageUrl("https://example.com/aodai.jpg")
                .description("Tuyệt tác áo dài thủ công")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(20L);
            return p;
        });
        when(sizeRepository.findByName("M")).thenReturn(Optional.of(sampleSize));
        when(colorRepository.findByName("Đen Onyx")).thenReturn(Optional.of(sampleColor));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant v = invocation.getArgument(0);
            v.setId(200L);
            return v;
        });

        ProductDetailResponse created = productService.createProduct(request);

        assertNotNull(created);
        assertEquals(20L, created.getId());
        assertEquals("Áo Dài Di Sản Thêu Tay", created.getName());
        assertNotNull(created.getVariants());
        assertEquals(1, created.getVariants().size(), "Phải tự động tạo sẵn 1 biến thể mặc định");
        assertEquals("M", created.getVariants().get(0).getSizeName());
        assertEquals("Đen Onyx", created.getVariants().get(0).getColorName());
        assertEquals(15, created.getVariants().get(0).getStockQuantity());
    }

    @Test
    @DisplayName("updateVariantStock cập nhật chính xác số lượng tồn kho")
    void updateVariantStock_ShouldUpdateAndReturnNewStock() {
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(sampleVariant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariantResponse updated = productService.updateVariantStock(100L, 50);

        assertNotNull(updated);
        assertEquals(50, updated.getStockQuantity());
        verify(productVariantRepository, times(1)).save(sampleVariant);
    }

    @Test
    @DisplayName("addVariant thêm biến thể mới cho sản phẩm thành công")
    void addVariant_ShouldAddVariantToProduct() {
        AdminCreateVariantRequest req = AdminCreateVariantRequest.builder()
                .sizeName("L")
                .colorName("Trắng Ngà")
                .colorHex("#FAF9F6")
                .stockQuantity(20)
                .additionalPrice(new BigDecimal("100000.00"))
                .build();

        Size sizeL = Size.builder().id(2L).name("L").build();
        Color colorWhite = Color.builder().id(2L).name("Trắng Ngà").hexCode("#FAF9F6").build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(sizeRepository.findByName("L")).thenReturn(Optional.of(sizeL));
        when(colorRepository.findByName("Trắng Ngà")).thenReturn(Optional.of(colorWhite));
        when(productVariantRepository.findByProductIdAndSizeIdAndColorId(10L, 2L, 2L)).thenReturn(Optional.empty());
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setId(201L);
            return v;
        });

        ProductVariantResponse result = productService.addVariant(10L, req);

        assertNotNull(result);
        assertEquals("L", result.getSizeName());
        assertEquals("Trắng Ngà", result.getColorName());
        assertEquals(20, result.getStockQuantity());
        assertEquals(new BigDecimal("100000.00"), result.getAdditionalPrice());
    }

    @Test
    @DisplayName("deleteVariant xóa biến thể sản phẩm khỏi cơ sở dữ liệu")
    void deleteVariant_ShouldRemoveVariant() {
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(sampleVariant));

        productService.deleteVariant(100L);

        verify(productVariantRepository, times(1)).delete(sampleVariant);
    }
}
