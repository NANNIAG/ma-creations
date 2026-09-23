package com.macreations.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.macreations.dto.CartItemResponse;
import com.macreations.dto.CartResponse;
import com.macreations.entity.Cart;
import com.macreations.entity.CartItem;
import com.macreations.entity.Product;
import com.macreations.entity.ProductImage;
import com.macreations.service.storage.ProductImageStorage;

@Component
public class CartMapper {

    public static final String CURRENCY_INR = "INR";
    private static final int MONEY_SCALE = 2;

    private final ProductImageStorage productImageStorage;

    public CartMapper(ProductImageStorage productImageStorage) {
        this.productImageStorage = productImageStorage;
    }

    public CartResponse emptyCart() {
        CartResponse response = new CartResponse();
        response.setId(null);
        response.setGuestToken(null);
        response.setItemCount(0);
        response.setSubtotal(money(BigDecimal.ZERO));
        response.setCurrency(CURRENCY_INR);
        response.setItems(List.of());
        return response;
    }

    public CartResponse toResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        // Only expose guest token for guest-owned carts (never for customer carts)
        response.setGuestToken(cart.getCustomer() == null ? cart.getGuestToken() : null);
        response.setCurrency(CURRENCY_INR);

        List<CartItemResponse> items = cart.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().isPublished())
                .map(this::toItemResponse)
                .toList();

        response.setItems(items);
        response.setItemCount(items.stream().mapToInt(CartItemResponse::getQuantity).sum());
        response.setSubtotal(items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return response;
    }

    public CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal unitPrice = money(product.getSellingPrice());
        int quantity = item.getQuantity();

        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(product.getId());
        response.setTitle(product.getTitle());
        response.setSlug(product.getSlug());
        response.setImageUrl(resolvePrimaryImageUrl(product));
        response.setUnitPrice(unitPrice);
        response.setMrp(money(product.getMrp()));
        response.setQuantity(quantity);
        response.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
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
