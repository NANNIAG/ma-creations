package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.ApiResponse;
import com.macreations.dto.CustomerOrderDetailResponse;
import com.macreations.dto.CustomerOrderListItemResponse;
import com.macreations.dto.PageResponse;
import com.macreations.service.CustomerOrderService;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerOrderListItemResponse>>> listOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(customerOrderService.listMyOrders(page, size)));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<CustomerOrderDetailResponse>> getOrder(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(customerOrderService.getMyOrder(orderNumber)));
    }
}
