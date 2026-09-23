package com.macreations.entity;

/**
 * Checkout payment methods. Pay Later is REQUIRED; provider integration is PENDING.
 * COD does not use an online payment gateway.
 */
public enum PaymentMethod {
    UPI,
    CARD,
    NET_BANKING,
    PAY_LATER,
    COD
}
