package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.AdminOrderDetailResponse;
import com.macreations.dto.AdminOrderListItemResponse;
import com.macreations.dto.ApiResponse;
import com.macreations.dto.PageResponse;
import com.macreations.dto.UpdateOrderStatusRequest;
import com.macreations.dto.UpdateOrderTrackingRequest;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentStatus;
import com.macreations.service.AdminOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminOrderListItemResponse>>> listOrders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOrderService.listOrders(q, status, paymentStatus, page, size)));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> getOrder(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(adminOrderService.getOrder(orderNumber)));
    }

    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> updateStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminOrderService.updateStatus(orderNumber, request)));
    }

    @PatchMapping("/{orderNumber}/tracking")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> updateTracking(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderTrackingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminOrderService.updateTracking(orderNumber, request)));
    }
}
