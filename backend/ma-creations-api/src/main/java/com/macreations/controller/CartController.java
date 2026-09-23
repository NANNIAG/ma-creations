package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.AddCartItemRequest;
import com.macreations.dto.ApiResponse;
import com.macreations.dto.CartResponse;
import com.macreations.dto.UpdateCartItemQuantityRequest;
import com.macreations.security.SecurityUtils;
import com.macreations.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    public static final String CART_TOKEN_HEADER = "X-Cart-Token";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(customerId, cartToken)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken,
            @Valid @RequestBody AddCartItemRequest request) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(cartService.addItem(customerId, cartToken, request)));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateQuantity(customerId, cartToken, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken,
            @PathVariable Long itemId) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(cartService.removeItem(customerId, cartToken, itemId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(cartService.clearCart(customerId, cartToken)));
    }

    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<CartResponse>> mergeGuestCart(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String cartToken) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(cartService.mergeGuestCart(customerId, cartToken)));
    }
}
