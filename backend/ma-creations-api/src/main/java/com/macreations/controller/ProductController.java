package com.macreations.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.ApiResponse;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.ProductListItemResponse;
import com.macreations.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductListItemResponse>>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(productService.listProducts(categoryId, sort, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getPublicProduct(id)));
    }
}
