package com.fashion.backend.config;

import com.fashion.backend.common.constant.GenderTarget;
import com.fashion.backend.common.constant.ProductStatus;
import com.fashion.backend.common.constant.RoleType;
import com.fashion.backend.modules.cart.entity.Cart;
import com.fashion.backend.modules.cart.repository.CartRepository;
import com.fashion.backend.modules.product.entity.*;
import com.fashion.backend.modules.product.repository.*;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting DataInitializer check for Neon PostgreSQL...");

        seedAdminUser();
        seedFashionCatalog();
        seedJeansCatalog();
        backfillLegacyJeans();

        log.info("DataInitializer completed successfully.");
    }

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@aifashion.com")) {
            User admin = User.builder()
                    .email("admin@aifashion.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản Trị Viên Atelier")
                    .role(RoleType.ROLE_ADMIN)
                    .build();
            User savedAdmin = userRepository.save(admin);

            Cart cart = Cart.builder().user(savedAdmin).build();
            cartRepository.save(cart);
            log.info("Seeded default admin user: admin@aifashion.com");
        }
    }

    private void seedFashionCatalog() {
        if (productRepository.count() >= 13) {
            log.info("Fashion catalog already contains {} products. Skipping seeding.", productRepository.count());
            return;
        }

        log.info("Seeding categories, sizes, colors and 13 Haute Couture products...");

        // 1. Categories
        Category catBlazer = getOrCreateCategory("Áo Khoác & Blazer", "Các thiết kế áo khoác măng tô, blazer may đo cấu trúc chuẩn mực");
        Category catSuit = getOrCreateCategory("Vest & Suit", "Bộ âu phục may đo bespoke thủ công đẳng cấp thượng lưu");
        Category catShirt = getOrCreateCategory("Áo Sơ Mi & Thun", "Áo sơ mi lụa, áo thun cotton nguyên bản và sweatshirt");
        Category catPants = getOrCreateCategory("Quần & Set May Đo", "Quần âu may đo, set trang phục đũi linen và phối đồ dạo phố");

        // 2. Sizes
        Size sizeS = getOrCreateSize("S");
        Size sizeM = getOrCreateSize("M");
        Size sizeL = getOrCreateSize("L");
        Size sizeXL = getOrCreateSize("XL");
        Size sizeXXL = getOrCreateSize("XXL");

        // 3. Colors
        Color cBlack = getOrCreateColor("Đen Onyx", "#121212");
        Color cWhite = getOrCreateColor("Trắng Ngà", "#FAFAFA");
        Color cGold = getOrCreateColor("Vàng Champagne", "#C5A880");
        Color cTeal = getOrCreateColor("Xanh Emerald", "#2A4843");
        Color cPlum = getOrCreateColor("Đỏ Mận Plum", "#63323E");

        // 4. Products & Variants (Mapped to the 13 images)
        List<ProductSeedDef> defs = getProductDefinitions(catBlazer, catSuit, catShirt, catPants);

        for (ProductSeedDef def : defs) {
            Product product = Product.builder()
                    .category(def.category)
                    .name(def.name)
                    .description(def.description)
                    .price(def.price)
                    .brand(def.brand)
                    .material(def.material)
                    .genderTarget(def.gender)
                    .style(def.style)
                    .occasion(def.occasion)
                    .imageUrl(def.imageUrl)
                    .status(ProductStatus.ACTIVE)
                    .build();

            Product savedProduct = productRepository.save(product);

            // Tạo các biến thể tương ứng
            int vIndex = 1;
            for (VariantSeedDef vDef : def.variants) {
                String sku = String.format("SKU-%d-%s-%s-%d", savedProduct.getId(), vDef.size.getName(), vDef.color.getName().substring(0, 2).toUpperCase(), vIndex++);
                ProductVariant variant = ProductVariant.builder()
                        .product(savedProduct)
                        .size(vDef.size)
                        .color(vDef.color)
                        .sku(sku)
                        .stockQuantity(vDef.stock)
                        .additionalPrice(vDef.additionalPrice)
                        .imageUrl(def.imageUrl)
                        .build();

                variantRepository.save(variant);
            }
        }

        log.info("Successfully seeded 13 luxury products and their variants into Neon DB.");
    }

    private Category getOrCreateCategory(String name, String desc) {
        return categoryRepository.findByName(name).orElseGet(() ->
                categoryRepository.save(Category.builder().name(name).description(desc).build())
        );
    }

    private Size getOrCreateSize(String name) {
        return sizeRepository.findByName(name).orElseGet(() ->
                sizeRepository.save(Size.builder().name(name).build())
        );
    }

    private Color getOrCreateColor(String name, String hex) {
        return colorRepository.findByName(name).orElseGet(() ->
                colorRepository.save(Color.builder().name(name).hexCode(hex).build())
        );
    }

    private static class VariantSeedDef {
        Size size;
        Color color;
        int stock;
        BigDecimal additionalPrice;

        VariantSeedDef(Size size, Color color, int stock, long addPrice) {
            this.size = size;
            this.color = color;
            this.stock = stock;
            this.additionalPrice = BigDecimal.valueOf(addPrice);
        }
    }

    private static class ProductSeedDef {
        Category category;
        String name;
        String description;
        BigDecimal price;
        String brand;
        String material;
        GenderTarget gender;
        String style;
        String occasion;
        String imageUrl;
        List<VariantSeedDef> variants = new ArrayList<>();

        ProductSeedDef(Category category, String name, String desc, long price, String brand, String material,
                       GenderTarget gender, String style, String occasion, String imageUrl) {
            this.category = category;
            this.name = name;
            this.description = desc;
            this.price = BigDecimal.valueOf(price);
            this.brand = brand;
            this.material = material;
            this.gender = gender;
            this.style = style;
            this.occasion = occasion;
            this.imageUrl = imageUrl;
        }

        ProductSeedDef addVariant(Size size, Color color, int stock, long addPrice) {
            variants.add(new VariantSeedDef(size, color, stock, addPrice));
            return this;
        }
    }

    private List<ProductSeedDef> getProductDefinitions(Category catBlazer, Category catSuit, Category catShirt, Category catPants) {
        Size sizeS = sizeRepository.findByName("S").orElseThrow();
        Size sizeM = sizeRepository.findByName("M").orElseThrow();
        Size sizeL = sizeRepository.findByName("L").orElseThrow();
        Size sizeXL = sizeRepository.findByName("XL").orElseThrow();

        Color cBlack = colorRepository.findByName("Đen Onyx").orElseThrow();
        Color cWhite = colorRepository.findByName("Trắng Ngà").orElseThrow();
        Color cGold = colorRepository.findByName("Vàng Champagne").orElseThrow();
        Color cTeal = colorRepository.findByName("Xanh Emerald").orElseThrow();
        Color cPlum = colorRepository.findByName("Đỏ Mận Plum").orElseThrow();

        List<ProductSeedDef> list = new ArrayList<>();

        // 1. minimal-white-blazer.jpg.jpg
        list.add(new ProductSeedDef(catBlazer, "Minimalist White Tailored Blazer",
                "Áo blazer may đo sắc trắng tinh khiết, ve áo sắc sảo, phom dáng cấu trúc chuẩn mực chuẩn thượng lưu.",
                1250000, "Bespoke Atelier", "Wool Blend & Lụa Tơ Tằm", GenderTarget.UNISEX, "MINIMALIST", "OFFICE_PARTY",
                "/src/assets/images/minimal-white-blazer.jpg.jpg")
                .addVariant(sizeS, cWhite, 15, 0)
                .addVariant(sizeM, cWhite, 25, 0)
                .addVariant(sizeL, cWhite, 10, 50000)
                .addVariant(sizeM, cBlack, 8, 0));

        // 2. teal-suit-luxury-woman.jpg.jpg
        list.add(new ProductSeedDef(catSuit, "Haute Couture Emerald Teal Suit",
                "Bộ suit cao cấp tone xanh ngọc Emerald quý phái, đường cắt may ôm đường nét cơ thể thanh lịch và quyền lực.",
                2450000, "Heritage Tailoring", "Italian Silk Wool", GenderTarget.WOMEN, "EDITORIAL_LUXURY", "GALA",
                "/src/assets/images/teal-suit-luxury-woman.jpg.jpg")
                .addVariant(sizeS, cTeal, 8, 0)
                .addVariant(sizeM, cTeal, 12, 0)
                .addVariant(sizeL, cTeal, 6, 100000));

        // 3. terracotta-bomber-jacket.jpg.jpg
        list.add(new ProductSeedDef(catBlazer, "Terracotta Silk Bomber Jacket",
                "Áo bomber lụa màu đất nung Terracotta ấm áp, chi tiết bo dệt kim thủ công tinh tế dành cho mùa thu đông.",
                1150000, "Urban Bespoke", "Satin Silk & Dệt Kim", GenderTarget.UNISEX, "MODERN_CLASSIC", "CASUAL_CHIC",
                "/src/assets/images/terracotta-bomber-jacket.jpg.jpg")
                .addVariant(sizeS, cGold, 10, 0)
                .addVariant(sizeM, cGold, 18, 0)
                .addVariant(sizeL, cGold, 12, 50000));

        // 4. editorial-white-suit-steps.jpg.jpg
        list.add(new ProductSeedDef(catSuit, "Editorial Bespoke White Steps Suit",
                "Bộ âu phục trắng ngà may đo độc bản, cảm hứng từ những bước chân bậc thang nghệ thuật kiến trúc Ý.",
                2850000, "Maison Minimal", "Premium Linen-Wool", GenderTarget.UNISEX, "BESPOKE", "CEREMONY",
                "/src/assets/images/editorial-white-suit-steps.jpg.jpg")
                .addVariant(sizeS, cWhite, 5, 0)
                .addVariant(sizeM, cWhite, 10, 0)
                .addVariant(sizeL, cWhite, 7, 100000));

        // 5. streetwear-beige-set.jpg.jpg
        list.add(new ProductSeedDef(catPants, "Contemporary Linen Beige Set",
                "Set trang phục đũi tự nhiên tone be đương đại, form dáng suông phóng khoáng cho những buổi dạo phố phong cách.",
                1350000, "Atelier Studio", "Natural Linen 100%", GenderTarget.UNISEX, "CASUAL_CHIC", "WEEKEND",
                "/src/assets/images/streetwear-beige-set.jpg.jpg")
                .addVariant(sizeS, cGold, 12, 0)
                .addVariant(sizeM, cGold, 20, 0)
                .addVariant(sizeL, cGold, 14, 50000)
                .addVariant(sizeXL, cGold, 6, 50000));

        // 6. streetwear-backprint-tee.jpg.jpg
        list.add(new ProductSeedDef(catShirt, "Atelier Graphic Backprint Oversized Tee",
                "Áo thun form rộng in đồ họa kiến trúc độc bản phía sau lưng, chất liệu cotton chải kỹ 250gsm dày dặn.",
                550000, "UrbanStyle", "Combed Cotton 100%", GenderTarget.UNISEX, "STREETWEAR", "CAFE",
                "/src/assets/images/streetwear-backprint-tee.jpg.jpg")
                .addVariant(sizeS, cBlack, 20, 0)
                .addVariant(sizeM, cBlack, 35, 0)
                .addVariant(sizeL, cBlack, 25, 0)
                .addVariant(sizeXL, cBlack, 15, 0));

        // 7. mockup-white-sweatshirt.jpg.jpg
        list.add(new ProductSeedDef(catShirt, "Minimalist Pure White Sweatshirt",
                "Áo nỉ chui đầu cổ tròn màu trắng tuyết, lót nỉ mềm mại giữ ấm hoàn hảo cho những ngày se lạnh.",
                690000, "BasicWear Atelier", "Heavyweight French Terry", GenderTarget.UNISEX, "MINIMALIST", "DAILY",
                "/src/assets/images/mockup-white-sweatshirt.jpg.jpg")
                .addVariant(sizeS, cWhite, 15, 0)
                .addVariant(sizeM, cWhite, 30, 0)
                .addVariant(sizeL, cWhite, 20, 0));

        // 8. urban-street-style.jpg.jpg
        list.add(new ProductSeedDef(catBlazer, "Urban Sophisticated Long Overcoat",
                "Áo măng tô dạ dáng dài cao cấp, thiết kế tối giản mang đậm phong cách đường phố London hiện đại.",
                2150000, "Bespoke Atelier", "Cashmere Wool Blend", GenderTarget.MEN, "MODERN_CLASSIC", "WINTER_BUSINESS",
                "/src/assets/images/urban-street-style.jpg.jpg")
                .addVariant(sizeM, cBlack, 10, 0)
                .addVariant(sizeL, cBlack, 14, 50000)
                .addVariant(sizeXL, cBlack, 8, 100000));

        // 9. flatlay-casual-outfit.jpg.jpg
        list.add(new ProductSeedDef(catPants, "Weekend Bespoke Casual Ensemble",
                "Set phối đồ nghỉ ngơi cuối tuần tinh tế gồm quần âu xếp ly và phụ kiện may đo hòa hợp nhịp sống thảnh thơi.",
                1450000, "Atelier Studio", "Cotton Linen", GenderTarget.UNISEX, "CASUAL_CHIC", "WEEKEND",
                "/src/assets/images/flatlay-casual-outfit.jpg.jpg")
                .addVariant(sizeS, cGold, 8, 0)
                .addVariant(sizeM, cGold, 15, 0)
                .addVariant(sizeL, cGold, 10, 50000));

        // 10. casual-beach-sunset-look.jpg.jpg
        list.add(new ProductSeedDef(catShirt, "Resort Linen Summer Sunset Shirt",
                "Áo sơ mi linen cổ Cuba thoáng mát dành cho kỳ nghỉ bãi biển hoàng hôn nhiệt đới lãng mạn.",
                750000, "Pure Linens", "Organic Linen", GenderTarget.MEN, "RELAXED_TAILORING", "VACATION",
                "/src/assets/images/casual-beach-sunset-look.jpg.jpg")
                .addVariant(sizeM, cWhite, 18, 0)
                .addVariant(sizeL, cWhite, 15, 0)
                .addVariant(sizeXL, cWhite, 10, 0));

        // 11. street-leather-bucket-hat.jpg.jpg
        list.add(new ProductSeedDef(catPants, "Artisan Nappa Leather Bucket Hat",
                "Nón bucket da Nappa chế tác thủ công bằng tay, điểm nhấn cá tính sang trọng cho bộ trang phục.",
                490000, "Artisan Leather Lab", "Nappa Leather", GenderTarget.UNISEX, "STREETWEAR_LUXURY", "CASUAL",
                "/src/assets/images/street-leather-bucket-hat.jpg.jpg")
                .addVariant(sizeS, cBlack, 12, 0)
                .addVariant(sizeM, cBlack, 20, 0)
                .addVariant(sizeL, cBlack, 0, 0)); // Tồn kho = 0 để test hết hàng

        // 12. hero-luxury-woman.jpg.jpg
        list.add(new ProductSeedDef(catSuit, "Atelier Signature Runway Gown",
                "Đầm dạ hội độc bản thêu tay chỉ vàng danh giá, xuất hiện tại tuần lễ thời trang Paris Haute Couture.",
                3900000, "Haute Couture Atelier", "Mulberry Silk & Organza", GenderTarget.WOMEN, "ROYAL_LUXURY", "GALA_RED_CARPET",
                "/src/assets/images/hero-luxury-woman.jpg.jpg")
                .addVariant(sizeS, cPlum, 4, 0)
                .addVariant(sizeM, cPlum, 6, 0)
                .addVariant(sizeL, cPlum, 3, 150000));

        // 13. wardrobe-clothing-rack.jpg.jpg
        list.add(new ProductSeedDef(catPants, "Studio Wardrobe Tailored Trousers",
                "Quần âu may đo ống đứng chuẩn phom, chất vải dệt mỏng nhẹ giữ nếp ly sắc bén cả ngày dài.",
                950000, "Bespoke Atelier", "Fine Tropical Wool", GenderTarget.UNISEX, "MINIMALIST", "OFFICE",
                "/src/assets/images/wardrobe-clothing-rack.jpg.jpg")
                .addVariant(sizeS, cBlack, 14, 0)
                .addVariant(sizeM, cBlack, 22, 0)
                .addVariant(sizeL, cBlack, 16, 30000)
                .addVariant(sizeXL, cBlack, 8, 50000));

        return list;
    }

    private void seedJeansCatalog() {
        Category catJeans = getOrCreateCategory("Quần Jeans", "Bộ sưu tập quần jean và denim may đo cao cấp");

        Size sizeS = getOrCreateSize("S");
        Size sizeM = getOrCreateSize("M");
        Size sizeL = getOrCreateSize("L");
        Size sizeXL = getOrCreateSize("XL");

        Color cDenim = getOrCreateColor("Xanh Denim Classic", "#2B4C7E");
        Color cIndigo = getOrCreateColor("Xanh Indigo Đậm", "#1A2B4C");
        Color cVintage = getOrCreateColor("Xanh Vintage Ripped", "#4A6B82");

        List<ProductSeedDef> list = new ArrayList<>();

        // 1. jean (1).jpg - Vintage Artisan Ripped Denim Jeans
        list.add(new ProductSeedDef(catJeans, "Vintage Artisan Ripped Denim Jeans",
                "Quần jean denim mài xước thủ công phong cách vintage bụi bặm, đường may chỉ đôi gia cố chắc chắn cùng chi tiết rách nghệ thuật.",
                1150000, "Urban Bespoke", "100% Selvedge Denim Cotton 13oz", GenderTarget.UNISEX, "STREETWEAR_LUXURY", "CASUAL_CHIC",
                "/src/assets/images/jean (1).jpg")
                .addVariant(sizeS, cVintage, 15, 0)
                .addVariant(sizeM, cVintage, 25, 0)
                .addVariant(sizeL, cVintage, 20, 50000)
                .addVariant(sizeXL, cVintage, 10, 50000));

        // 2. jean (2).jpg - Classic Straight Leg Rolled Denim Jeans
        list.add(new ProductSeedDef(catJeans, "Classic Straight Leg Rolled Denim Jeans",
                "Quần jean xanh ống đứng kinh điển tôn dáng với chi tiết xắn gấu sành điệu, chất liệu denim chéo co giãn nhẹ thoải mái suốt ngày dài.",
                1280000, "Bespoke Atelier", "Premium Stretch Denim (98% Cotton, 2% Spandex)", GenderTarget.WOMEN, "MODERN_CLASSIC", "DAILY",
                "/src/assets/images/jean (2).jpg")
                .addVariant(sizeS, cDenim, 18, 0)
                .addVariant(sizeM, cDenim, 30, 0)
                .addVariant(sizeL, cDenim, 22, 50000)
                .addVariant(sizeXL, cDenim, 12, 50000));

        // 3. jean (3).jpg - High-Waisted Contour Slim Fit Jeans
        list.add(new ProductSeedDef(catJeans, "High-Waisted Contour Slim Fit Jeans",
                "Quần jean cạp cao ôm đường nét cơ thể thanh lịch, hiệu ứng wash sáng nhẹ phần đùi tạo chiều sâu và tôn vinh vóc dáng người mặc.",
                1350000, "Heritage Tailoring", "Organic Indigo Denim 12.5oz", GenderTarget.WOMEN, "ELEGANT_CASUAL", "WEEKEND",
                "/src/assets/images/jean (3).jpg")
                .addVariant(sizeS, cIndigo, 12, 0)
                .addVariant(sizeM, cIndigo, 24, 0)
                .addVariant(sizeL, cIndigo, 16, 50000)
                .addVariant(sizeXL, cIndigo, 8, 50000));

        // 4. jean (4).jpg - Raw Selvedge Heavyweight Denim Trousers
        list.add(new ProductSeedDef(catJeans, "Raw Selvedge Heavyweight Denim Trousers",
                "Quần jean denim mộc thô dệt con thoi Nhật Bản chuẩn mực 14.5oz, phom ống suông đứng sắc nét dành cho giới sành thời trang nguyên bản.",
                1850000, "Atelier Studio", "Japanese Kurabo Selvedge Raw Denim 14.5oz", GenderTarget.MEN, "BESPOKE", "OFFICE_PARTY",
                "/src/assets/images/jean (4).jpg")
                .addVariant(sizeS, cIndigo, 10, 0)
                .addVariant(sizeM, cIndigo, 20, 0)
                .addVariant(sizeL, cIndigo, 15, 80000)
                .addVariant(sizeXL, cIndigo, 8, 80000));

        // 5. jean (5).jpg - Atelier Boutique Stacked Washed Jeans
        list.add(new ProductSeedDef(catJeans, "Atelier Boutique Stacked Washed Jeans",
                "Mẫu quần jean xanh nhạt wash đá phong cách Boutique cao cấp, gấu quần xếp nếp tự nhiên hòa quyện với phong cách dạo phố đương đại.",
                1420000, "Maison Minimal", "Stonewashed Turkish Cotton Denim", GenderTarget.UNISEX, "MINIMALIST", "CAFE",
                "/src/assets/images/jean (5).jpg")
                .addVariant(sizeS, cDenim, 14, 0)
                .addVariant(sizeM, cDenim, 28, 0)
                .addVariant(sizeL, cDenim, 18, 50000)
                .addVariant(sizeXL, cDenim, 10, 50000));

        // 6. jean (6).jpg - Haute Couture Feather-Touch Denim Trousers
        list.add(new ProductSeedDef(catJeans, "Haute Couture Feather-Touch Denim Trousers",
                "Thiết kế quần jean may đo độc bản xử lý sợi tơ tằm siêu mềm mại như lông vũ, đem lại sự êm ái vượt bậc mà vẫn giữ trọn phom dáng sắc nét.",
                2200000, "Haute Couture Atelier", "Silk-Infused Luxury Denim Blend", GenderTarget.UNISEX, "ROYAL_LUXURY", "GALA",
                "/src/assets/images/jean (6).jpg")
                .addVariant(sizeS, cDenim, 8, 0)
                .addVariant(sizeM, cDenim, 16, 0)
                .addVariant(sizeL, cDenim, 12, 100000)
                .addVariant(sizeXL, cDenim, 6, 100000));

        for (ProductSeedDef def : list) {
            if (!productRepository.existsByName(def.name)) {
                Product product = Product.builder()
                        .category(def.category)
                        .name(def.name)
                        .description(def.description)
                        .price(def.price)
                        .brand(def.brand)
                        .material(def.material)
                        .genderTarget(def.gender)
                        .style(def.style)
                        .occasion(def.occasion)
                        .imageUrl(def.imageUrl)
                        .status(ProductStatus.ACTIVE)
                        .build();

                Product savedProduct = productRepository.save(product);

                int vIndex = 1;
                for (VariantSeedDef vDef : def.variants) {
                    String sku = String.format("SKU-JEAN-%d-%s-%d", savedProduct.getId(), vDef.size.getName(), vIndex++);
                    ProductVariant variant = ProductVariant.builder()
                            .product(savedProduct)
                            .size(vDef.size)
                            .color(vDef.color)
                            .sku(sku)
                            .stockQuantity(vDef.stock)
                            .additionalPrice(vDef.additionalPrice)
                            .imageUrl(def.imageUrl)
                            .build();

                    variantRepository.save(variant);
                }
                log.info("Seeded jeans product: {} with {} variants", def.name, def.variants.size());
            }
        }
    }

    private void backfillLegacyJeans() {
        Category catJeans = getOrCreateCategory("Quần Jeans", "Bộ sưu tập quần jean và denim may đo cao cấp");
        Size sizeM = getOrCreateSize("M");
        Size sizeL = getOrCreateSize("L");
        Color cDenim = getOrCreateColor("Xanh Denim Classic", "#2B4C7E");

        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            String n = p.getName() != null ? p.getName().toLowerCase() : "";
            if (n.contains("jean") || n.contains("denim")) {
                boolean modified = false;

                // Đồng bộ về đúng danh mục "Quần Jeans"
                if (p.getCategory() == null || !p.getCategory().getId().equals(catJeans.getId())) {
                    p.setCategory(catJeans);
                    modified = true;
                }

                // Sửa ảnh chết hoặc thiếu ảnh
                if (p.getImageUrl() == null || p.getImageUrl().isBlank() || p.getImageUrl().contains("1542272604-780c96856592") || p.getImageUrl().contains("unsplash")) {
                    p.setImageUrl("/src/assets/images/jean (2).jpg");
                    modified = true;
                }

                if (modified) {
                    productRepository.save(p);
                    log.info("Synced jean product #{}: {} -> category {}", p.getId(), p.getName(), catJeans.getName());
                }

                // Kiểm tra nếu sản phẩm cũ chưa có biến thể nào
                List<ProductVariant> existingVariants = variantRepository.findByProductId(p.getId());
                if (existingVariants == null || existingVariants.isEmpty()) {
                    ProductVariant v1 = ProductVariant.builder()
                            .product(p)
                            .size(sizeM)
                            .color(cDenim)
                            .sku(String.format("SKU-LEGACY-%d-M-1", p.getId()))
                            .stockQuantity(25)
                            .additionalPrice(BigDecimal.ZERO)
                            .imageUrl(p.getImageUrl())
                            .build();
                    ProductVariant v2 = ProductVariant.builder()
                            .product(p)
                            .size(sizeL)
                            .color(cDenim)
                            .sku(String.format("SKU-LEGACY-%d-L-2", p.getId()))
                            .stockQuantity(20)
                            .additionalPrice(BigDecimal.valueOf(50000))
                            .imageUrl(p.getImageUrl())
                            .build();
                    variantRepository.save(v1);
                    variantRepository.save(v2);
                    log.info("Created default variants for legacy product #{}: {}", p.getId(), p.getName());
                }
            }
        }
    }
}
