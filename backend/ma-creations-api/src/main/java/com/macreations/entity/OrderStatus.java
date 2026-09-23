package com.macreations.entity;

/**
 * Fulfillment lifecycle for an order. Separate from {@link PaymentStatus}.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    PAYMENT_FAILED
}
