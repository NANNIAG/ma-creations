package com.macreations.entity;

/**
 * Online payment provider identity stored on {@link PaymentTransaction}.
 * COD uses {@link #NONE} (no Razorpay processing).
 */
public enum PaymentProvider {
    RAZORPAY,
    NONE
}
