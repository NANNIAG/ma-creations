package com.macreations.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.macreations.entity.Order;
import com.macreations.repository.OrderRepository;

/**
 * Order domain foundation for later placement/admin steps.
 * Does not expose public order-creation APIs in Step 21.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    public OrderService(OrderRepository orderRepository, OrderNumberGenerator orderNumberGenerator) {
        this.orderRepository = orderRepository;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByPlacedAtDesc(customerId);
    }

    /**
     * Allocates a unique customer-facing order number (for future place-order).
     */
    public String allocateOrderNumber() {
        return orderNumberGenerator.nextOrderNumber();
    }
}
