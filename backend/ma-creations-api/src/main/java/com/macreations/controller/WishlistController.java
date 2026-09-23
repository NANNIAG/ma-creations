package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.AddWishlistItemRequest;
import com.macreations.dto.ApiResponse;
import com.macreations.dto.WishlistResponse;
import com.macreations.security.SecurityUtils;
import com.macreations.service.WishlistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    public static final String WISHLIST_TOKEN_HEADER = "X-Wishlist-Token";

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
            @RequestHeader(value = WISHLIST_TOKEN_HEADER, required = false) String wishlistToken) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(customerId, wishlistToken)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<WishlistResponse>> addItem(
            @RequestHeader(value = WISHLIST_TOKEN_HEADER, required = false) String wishlistToken,
            @Valid @RequestBody AddWishlistItemRequest request) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.addItem(customerId, wishlistToken, request)));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeItem(
            @RequestHeader(value = WISHLIST_TOKEN_HEADER, required = false) String wishlistToken,
            @PathVariable Long productId) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.removeItem(customerId, wishlistToken, productId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> clearWishlist(
            @RequestHeader(value = WISHLIST_TOKEN_HEADER, required = false) String wishlistToken) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.clearWishlist(customerId, wishlistToken)));
    }

    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<WishlistResponse>> mergeGuestWishlist(
            @RequestHeader(value = WISHLIST_TOKEN_HEADER, required = false) String wishlistToken) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.mergeGuestWishlist(customerId, wishlistToken)));
    }
}
