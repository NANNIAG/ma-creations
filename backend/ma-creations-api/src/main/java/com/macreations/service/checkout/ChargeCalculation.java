package com.macreations.service.checkout;

import java.math.BigDecimal;

/**
 * Result of a charge calculator. When {@link #configured()} is false, amount must not be used for place-order.
 */
public record ChargeCalculation(
        boolean configured,
        BigDecimal amount,
        String code,
        String message
) {
    public static ChargeCalculation configured(BigDecimal amount) {
        return new ChargeCalculation(true, amount, null, null);
    }

    public static ChargeCalculation pending(String code, String message) {
        return new ChargeCalculation(false, null, code, message);
    }

    public static ChargeCalculation zero() {
        return configured(BigDecimal.ZERO.setScale(2));
    }
}
