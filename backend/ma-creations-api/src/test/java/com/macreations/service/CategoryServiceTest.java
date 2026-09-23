package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.dto.CategoryResponse;
import com.macreations.entity.Category;
import com.macreations.mapper.ProductMapper;
import com.macreations.repository.CategoryRepository;
import com.macreations.service.storage.ProductImageStorage;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductImageStorage productImageStorage;

    @Test
    void returnsCategoriesInDisplayOrder() {
        ProductMapper mapper = new ProductMapper(productImageStorage);
        CategoryService service = new CategoryService(categoryRepository, mapper);

        Category c1 = new Category();
        c1.setId(1L);
        c1.setName("Hydration & Drinkware");
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(c1));

        List<CategoryResponse> result = service.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Hydration & Drinkware");
    }
}
