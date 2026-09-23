package com.macreations.service.checkout;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.macreations.entity.PaymentMethod;

/**
 * Shipping charge calculator.
 * V1 client rule: flat ₹20 ({@code FIXED} + {@code fixed-amount}).
 * Modes: PENDING | ZERO | FIXED.
 */
@Component
public class ConfigurableShippingChargeCalculator implements ShippingChargeCalculator {

    private final String mode;
    private final BigDecimal fixedAmount;

    public ConfigurableShippingChargeCalculator(
            @Value("${app.checkout.shipping.mode:FIXED}") String mode,
            @Value("${app.checkout.shipping.fixed-amount:20.00}") BigDecimal fixedAmount) {
        this.mode = mode == null ? "FIXED" : mode.trim().toUpperCase();
        this.fixedAmount = fixedAmount == null
                ? new BigDecimal("20.00")
                : fixedAmount.setScale(2);
    }

    @Override
    public ChargeCalculation calculate(BigDecimal itemsSubtotal, PaymentMethod paymentMethod) {
        return switch (mode) {
            case "ZERO" -> ChargeCalculation.zero();
            case "FIXED" -> ChargeCalculation.configured(fixedAmount);
            default -> ChargeCalculation.pending(
                    "SHIPPING_RULE_PENDING",
                    "Shipping charge rule is not configured yet");
        };
    }
}
