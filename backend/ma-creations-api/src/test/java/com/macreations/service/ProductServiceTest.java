package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import com.macreations.dto.CreateProductRequest;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.ProductListItemResponse;
import com.macreations.dto.UpdateProductRequest;
import com.macreations.entity.Category;
import com.macreations.entity.Product;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.ProductMapper;
import com.macreations.repository.ProductRepository;
import com.macreations.service.storage.ProductImageStorage;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private ProductImageStorage productImageStorage;

    private ProductMapper productMapper;
    private ProductService productService;

    private Category category;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper(productImageStorage);
        productService = new ProductService(productRepository, categoryService, productMapper, productImageStorage);

        category = new Category();
        category.setId(1L);
        category.setName("Hydration & Drinkware");
        category.setSlug("hydration-drinkware");
        category.setDisplayOrder(1);
    }

    @Test
    void listsProductsNewestByDefault() {
        Product product = sampleProduct(10L, "Bottle", "80.00", "100.00");
        when(productRepository.findByPublishedTrue(any(Sort.class))).thenReturn(List.of(product));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        List<ProductListItemResponse> result = productService.listProducts(null, null);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(productRepository).findByPublishedTrue(sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDiscountPercent()).isEqualTo(20);
        assertThat(result.get(0).getPrimaryImageUrl()).isEqualTo("/api/media/img.jpg");
    }

    @Test
    void filtersByCategoryAndSortsPriceAsc() {
        when(categoryService.requireCategory(1L)).thenReturn(category);
        when(productRepository.findByCategoryIdAndPublishedTrue(eq(1L), any(Sort.class))).thenReturn(List.of());

        productService.listProducts(1L, "price_asc");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(productRepository).findByCategoryIdAndPublishedTrue(eq(1L), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("sellingPrice").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void sortsPriceDesc() {
        when(productRepository.findByPublishedTrue(any(Sort.class))).thenReturn(List.of());

        productService.listProducts(null, "price_desc");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(productRepository).findByPublishedTrue(sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("sellingPrice").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rejectsInvalidSort() {
        assertThatThrownBy(() -> productService.listProducts(null, "popular"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sort must be one of");
    }

    @Test
    void blankOrWhitespaceSearchUsesNormalListing() {
        when(productRepository.findByPublishedTrue(any(Sort.class))).thenReturn(List.of());

        productService.listProducts(null, "newest", "   ");
        productService.listProducts(null, "newest", "");
        productService.listProducts(null, "newest", null);

        verify(productRepository, org.mockito.Mockito.times(3)).findByPublishedTrue(any(Sort.class));
        verify(productRepository, org.mockito.Mockito.never()).search(any(), any());
    }

    @Test
    void searchUsesEscapedPatternAndSort() {
        Product product = sampleProduct(10L, "Water Bottle", "80.00", "100.00");
        when(productRepository.search(eq("%bottle%"), any(Sort.class))).thenReturn(List.of(product));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        List<ProductListItemResponse> result = productService.listProducts(null, "newest", "bottle");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Water Bottle");
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(productRepository).search(eq("%bottle%"), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void searchPassesOriginalCasingAndReliesOnLowerInQuery() {
        when(productRepository.search(eq("%BoTtLe%"), any(Sort.class))).thenReturn(List.of());

        productService.listProducts(null, "newest", "BoTtLe");

        verify(productRepository).search(eq("%BoTtLe%"), any(Sort.class));
    }

    @Test
    void partialSearchPassesSubstringPattern() {
        when(productRepository.search(eq("%bott%"), any(Sort.class))).thenReturn(List.of());

        productService.listProducts(null, "price_asc", "bott");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(productRepository).search(eq("%bott%"), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("sellingPrice").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void phraseSearchKeepsSpacesInPattern() {
        when(productRepository.search(eq("%water bottle%"), any(Sort.class))).thenReturn(List.of());

        productService.listProducts(null, "price_desc", "water bottle");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(productRepository).search(eq("%water bottle%"), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("sellingPrice").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void searchWithCategoryUsesCombinedRepositoryMethod() {
        when(categoryService.requireCategory(2L)).thenReturn(category);
        when(productRepository.searchByCategoryId(eq(2L), eq("%bottle%"), any(Sort.class)))
                .thenReturn(List.of());

        productService.listProducts(2L, "newest", "bottle");

        verify(productRepository).searchByCategoryId(eq(2L), eq("%bottle%"), any(Sort.class));
        verify(productRepository, org.mockito.Mockito.never()).search(any(), any());
    }

    @Test
    void searchEscapesLikeWildcards() {
        when(productRepository.search(eq("%100\\%\\_off%"), any(Sort.class))).thenReturn(List.of());

        productService.listProducts(null, "newest", "100%_off");

        verify(productRepository).search(eq("%100\\%\\_off%"), any(Sort.class));
    }

    @Test
    void rejectsSearchLongerThan100Characters() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> productService.listProducts(null, "newest", tooLong))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("INVALID_SEARCH");
    }

    @Test
    void omittedSearchKeepsCategoryFilter() {
        when(categoryService.requireCategory(1L)).thenReturn(category);
        when(productRepository.findByCategoryIdAndPublishedTrue(eq(1L), any(Sort.class))).thenReturn(List.of());

        productService.listProducts(1L, "newest", null);

        verify(productRepository).findByCategoryIdAndPublishedTrue(eq(1L), any(Sort.class));
    }

    @Test
    void getPublicProductReturnsDetail() {
        Product product = sampleProduct(5L, "Bottle", "80.00", "100.00");
        when(productRepository.findByIdAndPublishedTrue(5L)).thenReturn(Optional.of(product));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        ProductDetailResponse detail = productService.getPublicProduct(5L);

        assertThat(detail.getId()).isEqualTo(5L);
        assertThat(detail.getTitle()).isEqualTo("Bottle");
        assertThat(detail.getCategory().getName()).isEqualTo("Hydration & Drinkware");
        assertThat(detail.getImages()).hasSize(1);
    }

    @Test
    void getPublicProductNotFound() {
        when(productRepository.findByIdAndPublishedTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getPublicProduct(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getPublicProductHiddenReturnsNotFound() {
        when(productRepository.findByIdAndPublishedTrue(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getPublicProduct(5L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void getAdminProductReturnsHiddenProduct() {
        Product product = sampleProduct(5L, "Bottle", "80.00", "100.00");
        product.setPublished(false);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        ProductDetailResponse detail = productService.getAdminProduct(5L);

        assertThat(detail.getId()).isEqualTo(5L);
        assertThat(detail.getTitle()).isEqualTo("Bottle");
    }

    @Test
    void createProductPersistsWithGeneratedSlugAndImage() {
        when(categoryService.requireCategory(1L)).thenReturn(category);
        when(productRepository.existsBySlug("modern-bottle")).thenReturn(false);
        when(productImageStorage.store(any())).thenReturn("stored.jpg");
        when(productImageStorage.toPublicUrl("stored.jpg")).thenReturn("/api/media/stored.jpg");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(42L);
            return p;
        });

        CreateProductRequest request = new CreateProductRequest();
        request.setTitle("Modern Bottle");
        request.setCategoryId(1L);
        request.setSellingPrice(new BigDecimal("120.00"));
        request.setMrp(new BigDecimal("150.00"));

        MockMultipartFile image = new MockMultipartFile(
                "image", "bottle.jpg", "image/jpeg", new byte[] {1, 2, 3});

        ProductDetailResponse created = productService.createProduct(request, image);

        assertThat(created.getId()).isEqualTo(42L);
        assertThat(created.getTitle()).isEqualTo("Modern Bottle");
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getSlug()).isEqualTo("modern-bottle");
        assertThat(productCaptor.getValue().isPublished()).isTrue();
        assertThat(productCaptor.getValue().getImages()).hasSize(1);
    }

    @Test
    void listAdminProductsIncludesCategoryCreatedAtAndPublished() {
        Product product = sampleProduct(10L, "Bottle", "80.00", "100.00");
        product.setPublished(false);
        product.setCreatedAt(java.time.Instant.parse("2026-01-15T10:00:00Z"));
        when(productRepository.findAll(any(Sort.class))).thenReturn(List.of(product));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        var result = productService.listAdminProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory().getName()).isEqualTo("Hydration & Drinkware");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(java.time.Instant.parse("2026-01-15T10:00:00Z"));
        assertThat(result.get(0).getDiscountPercent()).isEqualTo(20);
        assertThat(result.get(0).isPublished()).isFalse();
    }

    @Test
    void updateProductChangesFieldsWithoutRequiringImage() {
        Product product = sampleProduct(5L, "Bottle", "80.00", "100.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(categoryService.requireCategory(1L)).thenReturn(category);
        when(productRepository.existsBySlugAndIdNot("new-bottle", 5L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        UpdateProductRequest request = new UpdateProductRequest();
        request.setTitle("New Bottle");
        request.setCategoryId(1L);
        request.setSellingPrice(new BigDecimal("90.00"));
        request.setMrp(new BigDecimal("110.00"));

        ProductDetailResponse updated = productService.updateProduct(5L, request, null);

        assertThat(updated.getTitle()).isEqualTo("New Bottle");
        assertThat(updated.getSellingPrice()).isEqualByComparingTo("90.00");
        assertThat(product.getImages()).hasSize(1);
        verify(productImageStorage, org.mockito.Mockito.never()).store(any());
    }

    @Test
    void updateProductReplacesImageWhenProvided() {
        Product product = sampleProduct(5L, "Bottle", "80.00", "100.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(categoryService.requireCategory(1L)).thenReturn(category);
        when(productImageStorage.store(any())).thenReturn("new.jpg");
        when(productImageStorage.toPublicUrl("new.jpg")).thenReturn("/api/media/new.jpg");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest();
        request.setTitle("Bottle");
        request.setCategoryId(1L);
        request.setSellingPrice(new BigDecimal("80.00"));
        request.setMrp(new BigDecimal("100.00"));

        productService.updateProduct(
                5L,
                request,
                new MockMultipartFile("image", "b.jpg", "image/jpeg", new byte[] {9}));

        verify(productImageStorage).delete("img.jpg");
        verify(productImageStorage).store(any());
        assertThat(product.getImages()).hasSize(1);
        assertThat(product.getImages().get(0).getStoragePath()).isEqualTo("new.jpg");
    }

    @Test
    void updateProductStatusHidesProduct() {
        Product product = sampleProduct(5L, "Bottle", "80.00", "100.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        var request = new com.macreations.dto.UpdateProductStatusRequest();
        request.setPublished(false);

        var result = productService.updateProductStatus(5L, request);

        assertThat(product.isPublished()).isFalse();
        assertThat(result.isPublished()).isFalse();
        assertThat(result.getTitle()).isEqualTo("Bottle");
    }

    @Test
    void updateProductStatusPublishesProduct() {
        Product product = sampleProduct(5L, "Bottle", "80.00", "100.00");
        product.setPublished(false);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        var request = new com.macreations.dto.UpdateProductStatusRequest();
        request.setPublished(true);

        var result = productService.updateProductStatus(5L, request);

        assertThat(product.isPublished()).isTrue();
        assertThat(result.isPublished()).isTrue();
    }

    @Test
    void createProductFailsWhenCategoryMissing() {
        when(categoryService.requireCategory(99L))
                .thenThrow(new NotFoundException("CATEGORY_NOT_FOUND", "Category not found: 99"));

        CreateProductRequest request = new CreateProductRequest();
        request.setTitle("X");
        request.setCategoryId(99L);
        request.setSellingPrice(BigDecimal.ONE);
        request.setMrp(BigDecimal.TEN);

        assertThatThrownBy(() -> productService.createProduct(
                        request,
                        new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[] {1})))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    private Product sampleProduct(Long id, String title, String selling, String mrp) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setSellingPrice(new BigDecimal(selling));
        product.setMrp(new BigDecimal(mrp));
        product.setSlug(title.toLowerCase().replace(' ', '-'));
        product.setCategory(category);
        product.setPublished(true);

        var image = new com.macreations.entity.ProductImage();
        image.setId(1L);
        image.setProduct(product);
        image.setStoragePath("img.jpg");
        image.setSortOrder(0);
        product.getImages().add(image);
        return product;
    }
}
