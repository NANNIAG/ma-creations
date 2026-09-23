package com.macreations.entity;

/**
 * Payment lifecycle for an order. Separate from {@link OrderStatus}.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    COD_PENDING
}
