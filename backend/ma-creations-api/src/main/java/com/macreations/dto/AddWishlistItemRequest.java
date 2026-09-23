package com.macreations.dto;

import jakarta.validation.constraints.NotNull;

public class AddWishlistItemRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
