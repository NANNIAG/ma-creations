package com.macreations.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.macreations.dto.CategoryResponse;
import com.macreations.entity.Category;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.ProductMapper;
import com.macreations.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public CategoryService(CategoryRepository categoryRepository, ProductMapper productMapper) {
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(productMapper::toCategoryResponse)
                .toList();
    }

    public Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "CATEGORY_NOT_FOUND",
                        "Category not found: " + categoryId));
    }
}
