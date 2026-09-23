package com.macreations.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.ApiResponse;
import com.macreations.dto.GuestOrderTrackingResponse;
import com.macreations.dto.OrderSummaryResponse;
import com.macreations.dto.PlaceOrderRequest;
import com.macreations.dto.PlaceOrderResponse;
import com.macreations.service.GuestOrderTrackingService;
import com.macreations.service.OrderPlacementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderPlacementService orderPlacementService;
    private final GuestOrderTrackingService guestOrderTrackingService;

    public OrderController(
            OrderPlacementService orderPlacementService,
            GuestOrderTrackingService guestOrderTrackingService) {
        this.orderPlacementService = orderPlacementService;
        this.guestOrderTrackingService = guestOrderTrackingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlaceOrderResponse>> placeOrder(
            @RequestHeader(value = CartController.CART_TOKEN_HEADER, required = false) String cartToken,
            @Valid @RequestBody PlaceOrderRequest request) {
        PlaceOrderResponse placed = orderPlacementService.placeOrder(cartToken, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(placed));
    }

    /**
     * Guest order tracking: order number + mobile required.
     * Registered-customer orders are never returned here.
     */
    @GetMapping("/track")
    public ResponseEntity<ApiResponse<GuestOrderTrackingResponse>> trackGuestOrder(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String mobileNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                guestOrderTrackingService.track(orderNumber, mobileNumber)));
    }

    /**
     * Lightweight confirmation summary for success/failure pages.
     * Not a full order-history or guest-tracking API.
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderSummaryResponse>> getOrderSummary(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderPlacementService.getOrderSummary(orderNumber)));
    }
}
