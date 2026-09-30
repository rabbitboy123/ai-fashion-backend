package com.fashion.backend.modules.product.repository;

import com.fashion.backend.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findByProductIdAndSizeIdAndColorId(Long productId, Long sizeId, Long colorId);
}
