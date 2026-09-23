package com.macreations.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.macreations.dto.CustomerOrderDetailResponse;
import com.macreations.dto.CustomerOrderListItemResponse;
import com.macreations.dto.PageResponse;
import com.macreations.entity.Order;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.ForbiddenException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.OrderRepository;
import com.macreations.security.SecurityUtils;

@Service
public class CustomerOrderService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderDtoMapper orderDtoMapper;

    public CustomerOrderService(OrderRepository orderRepository, OrderDtoMapper orderDtoMapper) {
        this.orderRepository = orderRepository;
        this.orderDtoMapper = orderDtoMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerOrderListItemResponse> listMyOrders(Integer page, Integer size) {
        Long customerId = requireCustomerId();
        int pageIndex = page == null || page < 0 ? 0 : page;
        int pageSize = normalizeSize(size);
        Page<Order> result = orderRepository.findByCustomerIdOrderByPlacedAtDesc(
                customerId, PageRequest.of(pageIndex, pageSize));
        // Touch items for itemCount without N+1 open session issues
        result.getContent().forEach(o -> {
            if (o.getItems() != null) {
                o.getItems().size();
            }
        });
        return PageResponse.of(
                result.getContent().stream().map(orderDtoMapper::toCustomerListItem).toList(),
                pageIndex,
                pageSize,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse getMyOrder(String orderNumber) {
        Long customerId = requireCustomerId();
        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        if (order.getCustomer() == null || !customerId.equals(order.getCustomer().getId())) {
            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
        }
        if (order.getItems() != null) {
            order.getItems().size();
        }
        if (order.getShippingAddress() != null) {
            order.getShippingAddress().getCity();
        }
        return orderDtoMapper.toCustomerDetail(order);
    }

    private Long requireCustomerId() {
        return SecurityUtils.currentCustomerId()
                .orElseThrow(() -> new ForbiddenException("CUSTOMER_AUTH_REQUIRED", "Customer authentication is required"));
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "page size must be at most " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
