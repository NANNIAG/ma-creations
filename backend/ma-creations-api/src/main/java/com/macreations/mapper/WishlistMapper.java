package com.macreations.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.macreations.dto.WishlistItemResponse;
import com.macreations.dto.WishlistResponse;
import com.macreations.entity.Product;
import com.macreations.entity.ProductImage;
import com.macreations.entity.Wishlist;
import com.macreations.entity.WishlistItem;
import com.macreations.service.storage.ProductImageStorage;
import com.macreations.util.CatalogUtils;

@Component
public class WishlistMapper {

    private static final int MONEY_SCALE = 2;

    private final ProductImageStorage productImageStorage;

    public WishlistMapper(ProductImageStorage productImageStorage) {
        this.productImageStorage = productImageStorage;
    }

    public WishlistResponse emptyWishlist() {
        WishlistResponse response = new WishlistResponse();
        response.setId(null);
        response.setGuestToken(null);
        response.setItemCount(0);
        response.setItems(List.of());
        return response;
    }

    public WishlistResponse toResponse(Wishlist wishlist) {
        WishlistResponse response = new WishlistResponse();
        response.setId(wishlist.getId());
        response.setGuestToken(wishlist.getCustomer() == null ? wishlist.getGuestToken() : null);

        List<WishlistItemResponse> items = wishlist.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().isPublished())
                .map(this::toItemResponse)
                .toList();

        response.setItems(items);
        response.setItemCount(items.size());
        return response;
    }

    public WishlistItemResponse toItemResponse(WishlistItem item) {
        Product product = item.getProduct();

        WishlistItemResponse response = new WishlistItemResponse();
        response.setId(item.getId());
        response.setProductId(product.getId());
        response.setTitle(product.getTitle());
        response.setSlug(product.getSlug());
        response.setImageUrl(resolvePrimaryImageUrl(product));
        response.setUnitPrice(money(product.getSellingPrice()));
        response.setMrp(money(product.getMrp()));
        response.setDiscountPercent(
                CatalogUtils.calculateDiscountPercent(product.getSellingPrice(), product.getMrp()));
        return response;
    }

    private String resolvePrimaryImageUrl(Product product) {
        return product.getImages().stream()
                .min(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(image -> productImageStorage.toPublicUrl(image.getStoragePath()))
                .orElse(null);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
