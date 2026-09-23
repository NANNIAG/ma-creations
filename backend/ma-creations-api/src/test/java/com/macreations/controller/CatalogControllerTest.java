package com.macreations.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.macreations.dto.AdminProductListItemResponse;
import com.macreations.dto.CategoryResponse;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.ProductListItemResponse;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.exception.NotFoundException;
import com.macreations.security.AdminUserDetailsService;
import com.macreations.security.JsonAuthenticationEntryPoint;
import com.macreations.security.JwtService;
import com.macreations.service.CategoryService;
import com.macreations.service.ProductService;

@WebMvcTest(controllers = {
        CategoryController.class,
        ProductController.class,
        AdminProductController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    @Test
    void getCategories() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(
                new CategoryResponse(1L, "Hydration & Drinkware"),
                new CategoryResponse(2L, "Lunch & Meal Prep")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Hydration & Drinkware"));
    }

    @Test
    void getProducts() throws Exception {
        ProductListItemResponse item = new ProductListItemResponse();
        item.setId(1L);
        item.setTitle("Bottle");
        item.setSellingPrice(new BigDecimal("80.00"));
        item.setMrp(new BigDecimal("100.00"));
        item.setDiscountPercent(20);
        when(productService.listProducts(isNull(), eq("newest"), isNull())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Bottle"))
                .andExpect(jsonPath("$.data[0].discountPercent").value(20));
    }

    @Test
    void getProductsWithCategoryAndSort() throws Exception {
        when(productService.listProducts(1L, "price_asc", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/products").param("categoryId", "1").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getProductsWithSearch() throws Exception {
        ProductListItemResponse item = new ProductListItemResponse();
        item.setId(1L);
        item.setTitle("Water Bottle");
        when(productService.listProducts(isNull(), eq("newest"), eq("bottle"))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/products").param("search", "bottle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Water Bottle"));
    }

    @Test
    void getProductsWithSearchCategoryAndSort() throws Exception {
        when(productService.listProducts(2L, "price_asc", "bottle")).thenReturn(List.of());

        mockMvc.perform(get("/api/products")
                        .param("search", "bottle")
                        .param("categoryId", "2")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getProductById() throws Exception {
        ProductDetailResponse detail = new ProductDetailResponse();
        detail.setId(7L);
        detail.setTitle("Bottle");
        detail.setCategory(new CategoryResponse(1L, "Hydration & Drinkware"));
        when(productService.getPublicProduct(7L)).thenReturn(detail);

        mockMvc.perform(get("/api/products/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.category.name").value("Hydration & Drinkware"));
    }

    @Test
    void getProductNotFound() throws Exception {
        when(productService.getPublicProduct(404L))
                .thenThrow(new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: 404"));

        mockMvc.perform(get("/api/products/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void createProduct() throws Exception {
        ProductDetailResponse created = new ProductDetailResponse();
        created.setId(11L);
        created.setTitle("New Bottle");
        when(productService.createProduct(any(), any())).thenReturn(created);

        MockMultipartFile image = new MockMultipartFile(
                "image", "bottle.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("title", "New Bottle")
                        .param("categoryId", "1")
                        .param("sellingPrice", "120.00")
                        .param("mrp", "150.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.title").value("New Bottle"));
    }

    @Test
    void createProductValidationFailure() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "bottle.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("title", "")
                        .param("categoryId", "1")
                        .param("sellingPrice", "120.00")
                        .param("mrp", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createProductInvalidCategory() throws Exception {
        when(productService.createProduct(any(), any()))
                .thenThrow(new NotFoundException("CATEGORY_NOT_FOUND", "Category not found: 99"));

        MockMultipartFile image = new MockMultipartFile(
                "image", "bottle.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("title", "Bottle")
                        .param("categoryId", "99")
                        .param("sellingPrice", "10")
                        .param("mrp", "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void adminListProducts() throws Exception {
        AdminProductListItemResponse item = new AdminProductListItemResponse();
        item.setId(3L);
        item.setTitle("Candle");
        item.setCategory(new CategoryResponse(5L, "Home Decor & Festivity"));
        item.setSellingPrice(new BigDecimal("200.00"));
        item.setMrp(new BigDecimal("250.00"));
        item.setDiscountPercent(20);
        item.setCreatedAt(Instant.parse("2026-03-01T12:00:00Z"));
        item.setPublished(true);
        when(productService.listAdminProducts()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Candle"))
                .andExpect(jsonPath("$.data[0].category.name").value("Home Decor & Festivity"))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].published").value(true));
    }

    @Test
    void adminGetProduct() throws Exception {
        ProductDetailResponse detail = new ProductDetailResponse();
        detail.setId(3L);
        detail.setTitle("Candle");
        when(productService.getAdminProduct(3L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Candle"));
    }

    @Test
    void adminUpdateProduct() throws Exception {
        ProductDetailResponse updated = new ProductDetailResponse();
        updated.setId(3L);
        updated.setTitle("Updated Candle");
        when(productService.updateProduct(eq(3L), any(), isNull())).thenReturn(updated);

        MockMultipartHttpServletRequestBuilder request = multipart("/api/admin/products/3");
        request.with(r -> {
            r.setMethod("PUT");
            return r;
        });

        mockMvc.perform(request
                        .param("title", "Updated Candle")
                        .param("categoryId", "5")
                        .param("sellingPrice", "210")
                        .param("mrp", "260"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Candle"));
    }

    @Test
    void adminUpdateProductStatus() throws Exception {
        AdminProductListItemResponse item = new AdminProductListItemResponse();
        item.setId(3L);
        item.setTitle("Candle");
        item.setPublished(false);
        when(productService.updateProductStatus(eq(3L), any())).thenReturn(item);

        mockMvc.perform(patch("/api/admin/products/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.title").value("Candle"));
    }
}
