package com.macreations.entity;

/**
 * Lifecycle of a single payment attempt. Maps into order {@link PaymentStatus} in PaymentService.
 * Provider-specific states stay here — not on the Order domain.
 */
public enum PaymentTransactionStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    CANCELLED
}
