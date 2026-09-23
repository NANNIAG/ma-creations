package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.controller.CartController;
import com.macreations.dto.ApiResponse;
import com.macreations.dto.CheckoutPreviewRequest;
import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.security.SecurityUtils;
import com.macreations.service.checkout.CheckoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> preview(
            @RequestHeader(value = CartController.CART_TOKEN_HEADER, required = false) String cartToken,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        CheckoutPreviewResponse preview = checkoutService.preview(customerId, cartToken, request);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }
}
