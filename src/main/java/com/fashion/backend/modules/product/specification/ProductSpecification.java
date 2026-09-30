package com.fashion.backend.modules.product.specification;

import com.fashion.backend.common.constant.ProductStatus;
import com.fashion.backend.modules.product.dto.request.ProductFilterRequest;
import com.fashion.backend.modules.product.entity.Product;
import com.fashion.backend.modules.product.entity.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filter(ProductFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy sản phẩm ACTIVE
            predicates.add(criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));

            if (request == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Tìm kiếm từ khóa (tên, mô tả, thương hiệu)
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
                Predicate descLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
                Predicate brandLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), pattern);
                predicates.add(criteriaBuilder.or(nameLike, descLike, brandLike));
            }

            // 3. Lọc theo Danh mục
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // 4. Lọc theo khoảng giá
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // 5. Lọc theo Phong cách & Dịp sử dụng
            if (request.getStyle() != null && !request.getStyle().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("style")), request.getStyle().trim().toLowerCase()));
            }
            if (request.getOccasion() != null && !request.getOccasion().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("occasion")), request.getOccasion().trim().toLowerCase()));
            }

            // 6. Lọc theo Giới tính mục tiêu (MEN/WOMEN bao gồm cả thiết kế UNISEX)
            if (request.getGenderTarget() != null) {
                if (request.getGenderTarget() == com.fashion.backend.common.constant.GenderTarget.UNISEX) {
                    predicates.add(criteriaBuilder.equal(root.get("genderTarget"), com.fashion.backend.common.constant.GenderTarget.UNISEX));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("genderTarget"), request.getGenderTarget()),
                            criteriaBuilder.equal(root.get("genderTarget"), com.fashion.backend.common.constant.GenderTarget.UNISEX)
                    ));
                }
            }

            // 7. Lọc theo Màu sắc hoặc Kích thước (Join với bảng ProductVariant)
            if (request.getColorId() != null || request.getSizeId() != null) {
                assert query != null;
                query.distinct(true);
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);

                if (request.getColorId() != null) {
                    predicates.add(criteriaBuilder.equal(variantJoin.get("color").get("id"), request.getColorId()));
                }
                if (request.getSizeId() != null) {
                    predicates.add(criteriaBuilder.equal(variantJoin.get("size").get("id"), request.getSizeId()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
