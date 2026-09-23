package com.macreations.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.macreations.dto.AdminProductListItemResponse;
import com.macreations.dto.CategoryResponse;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.ProductImageResponse;
import com.macreations.dto.ProductListItemResponse;
import com.macreations.entity.Category;
import com.macreations.entity.Product;
import com.macreations.entity.ProductImage;
import com.macreations.service.storage.ProductImageStorage;
import com.macreations.util.CatalogUtils;

@Component
public class ProductMapper {

    private final ProductImageStorage productImageStorage;

    public ProductMapper(ProductImageStorage productImageStorage) {
        this.productImageStorage = productImageStorage;
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    public ProductListItemResponse toListItem(Product product) {
        ProductListItemResponse response = new ProductListItemResponse();
        response.setId(product.getId());
        response.setTitle(product.getTitle());
        response.setSellingPrice(product.getSellingPrice());
        response.setMrp(product.getMrp());
        response.setDiscountPercent(CatalogUtils.calculateDiscountPercent(product.getSellingPrice(), product.getMrp()));
        response.setAverageRating(product.getAverageRating());
        response.setRatingCount(product.getRatingCount());
        response.setPrimaryImageUrl(resolvePrimaryImageUrl(product));
        return response;
    }

    public AdminProductListItemResponse toAdminListItem(Product product) {
        AdminProductListItemResponse response = new AdminProductListItemResponse();
        response.setId(product.getId());
        response.setTitle(product.getTitle());
        response.setCategory(toCategoryResponse(product.getCategory()));
        response.setSellingPrice(product.getSellingPrice());
        response.setMrp(product.getMrp());
        response.setDiscountPercent(CatalogUtils.calculateDiscountPercent(product.getSellingPrice(), product.getMrp()));
        response.setAverageRating(product.getAverageRating());
        response.setRatingCount(product.getRatingCount());
        response.setPrimaryImageUrl(resolvePrimaryImageUrl(product));
        response.setCreatedAt(product.getCreatedAt());
        response.setPublished(product.isPublished());
        return response;
    }

    public ProductDetailResponse toDetail(Product product) {
        ProductDetailResponse response = new ProductDetailResponse();
        response.setId(product.getId());
        response.setTitle(product.getTitle());
        response.setSellingPrice(product.getSellingPrice());
        response.setMrp(product.getMrp());
        response.setDiscountPercent(CatalogUtils.calculateDiscountPercent(product.getSellingPrice(), product.getMrp()));
        response.setAverageRating(product.getAverageRating());
        response.setRatingCount(product.getRatingCount());
        response.setCategory(toCategoryResponse(product.getCategory()));

        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(image -> new ProductImageResponse(
                        image.getId(),
                        productImageStorage.toPublicUrl(image.getStoragePath()),
                        image.getSortOrder()))
                .toList();
        response.setImages(images);
        return response;
    }

    private String resolvePrimaryImageUrl(Product product) {
        return product.getImages().stream()
                .min(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(image -> productImageStorage.toPublicUrl(image.getStoragePath()))
                .orElse(null);
    }
}
