package com.fashion.backend.modules.product.repository;

import com.fashion.backend.common.constant.ProductStatus;
import com.fashion.backend.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    List<Product> findByCategoryId(Long categoryId);
    boolean existsByName(String name);
    List<Product> findByNameContainingIgnoreCase(String keyword);
}
