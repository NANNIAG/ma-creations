package com.macreations.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.macreations.dto.AdminProductListItemResponse;
import com.macreations.dto.ApiResponse;
import com.macreations.dto.CreateProductRequest;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.UpdateProductRequest;
import com.macreations.dto.UpdateProductStatusRequest;
import com.macreations.service.ProductService;

import jakarta.validation.Valid;

/**
 * Admin product management. All endpoints require authenticated ROLE_ADMIN (JWT).
 * Visibility: PATCH status (Hide/Publish). Hard delete is not part of the normal flow.
 */
@RestController
@RequestMapping("/api/admin/products")
@Validated
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminProductListItemResponse>>> listProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.listAdminProducts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAdminProduct(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @ModelAttribute CreateProductRequest request,
            @RequestPart("image") MultipartFile image) {
        ProductDetailResponse created = productService.createProduct(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ProductDetailResponse updated = productService.updateProduct(id, request, image);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminProductListItemResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProductStatus(id, request)));
    }
}
