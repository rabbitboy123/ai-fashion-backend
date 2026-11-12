package com.fashion.backend.modules.product.service;

import com.fashion.backend.modules.product.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);
}