package com.macreations.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.macreations.entity.OrderStatus;
import com.macreations.exception.BadRequestException;

/**
 * Admin fulfillment transitions only. Payment-driven statuses are not set here.
 * Cancellation/refund paths are not invented — CANCELLED is rejected until policy exists.
 */
@Component
public class OrderStatusTransitionValidator {

    private final Map<OrderStatus, Set<OrderStatus>> allowed = new EnumMap<>(OrderStatus.class);

    public OrderStatusTransitionValidator() {
        allowed.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.PROCESSING));
        allowed.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED));
        allowed.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        allowed.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        allowed.put(OrderStatus.PENDING_PAYMENT, EnumSet.noneOf(OrderStatus.class));
        allowed.put(OrderStatus.PAYMENT_FAILED, EnumSet.noneOf(OrderStatus.class));
        allowed.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public void validate(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            throw new BadRequestException("INVALID_STATUS", "Order status is required");
        }
        if (from == to) {
            return;
        }
        if (to == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "CANCEL_NOT_SUPPORTED",
                    "Cancellation is not supported until cancel/refund rules are confirmed");
        }
        if (to == OrderStatus.PENDING_PAYMENT || to == OrderStatus.PAYMENT_FAILED) {
            throw new BadRequestException(
                    "PAYMENT_STATUS_PROTECTED",
                    "Payment-related order statuses are controlled by the payment flow");
        }
        Set<OrderStatus> next = allowed.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
        if (!next.contains(to)) {
            throw new BadRequestException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot change order status from " + from + " to " + to);
        }
    }
}
