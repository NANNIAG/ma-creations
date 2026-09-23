package com.macreations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.macreations.dto.GuestOrderTrackingResponse;
import com.macreations.entity.Order;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.OrderRepository;
import com.macreations.util.MobileNumberUtils;

/**
 * Guest order lookup by order number + mobile. Does not serve registered-customer orders.
 * Wrong mobile / registered order / missing order all map to the same not-found response.
 */
@Service
public class GuestOrderTrackingService {

    private final OrderRepository orderRepository;
    private final OrderDtoMapper orderDtoMapper;

    public GuestOrderTrackingService(OrderRepository orderRepository, OrderDtoMapper orderDtoMapper) {
        this.orderRepository = orderRepository;
        this.orderDtoMapper = orderDtoMapper;
    }

    @Transactional(readOnly = true)
    public GuestOrderTrackingResponse track(String orderNumberRaw, String mobileNumberRaw) {
        if (!StringUtils.hasText(orderNumberRaw)) {
            throw new BadRequestException("ORDER_NUMBER_REQUIRED", "orderNumber is required");
        }
        String orderNumber = orderNumberRaw.trim();
        if (orderNumber.isEmpty()) {
            throw new BadRequestException("ORDER_NUMBER_REQUIRED", "orderNumber is required");
        }
        // Normalize (and validate) mobile before lookup; blank/invalid → BadRequest, not existence leak.
        String mobile = MobileNumberUtils.normalize(mobileNumberRaw);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(this::notFound);

        // Registered customer orders are never exposed on the guest path.
        if (order.getCustomer() != null) {
            throw notFound();
        }

        if (!mobileMatchesGuestOrder(order, mobile)) {
            throw notFound();
        }

        if (order.getItems() != null) {
            order.getItems().size();
        }
        if (order.getShippingAddress() != null) {
            order.getShippingAddress().getCity();
        }
        return orderDtoMapper.toGuestTracking(order);
    }

    private boolean mobileMatchesGuestOrder(Order order, String normalizedMobile) {
        if (normalizedMobile.equals(order.getContactMobile())) {
            return true;
        }
        return order.getGuestMobile() != null && normalizedMobile.equals(order.getGuestMobile());
    }

    private NotFoundException notFound() {
        return new NotFoundException("ORDER_NOT_FOUND", "Order not found");
    }
}
