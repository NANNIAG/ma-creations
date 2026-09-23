package com.macreations.service.checkout;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.macreations.entity.PaymentMethod;

/**
 * COD charge calculator.
 * V1 client rule: ₹20 when {@link PaymentMethod#COD}; otherwise ₹0.
 * Modes: PENDING | ZERO | FIXED (amount applied only for COD).
 */
@Component
public class ConfigurableCodChargeCalculator implements CodChargeCalculator {

    private final String mode;
    private final BigDecimal fixedAmount;

    public ConfigurableCodChargeCalculator(
            @Value("${app.checkout.cod.mode:FIXED}") String mode,
            @Value("${app.checkout.cod.fixed-amount:20.00}") BigDecimal fixedAmount) {
        this.mode = mode == null ? "FIXED" : mode.trim().toUpperCase();
        this.fixedAmount = fixedAmount == null
                ? new BigDecimal("20.00")
                : fixedAmount.setScale(2);
    }

    @Override
    public ChargeCalculation calculate(BigDecimal itemsSubtotal, PaymentMethod paymentMethod) {
        if (paymentMethod != PaymentMethod.COD) {
            return ChargeCalculation.zero();
        }
        return switch (mode) {
            case "ZERO" -> ChargeCalculation.zero();
            case "FIXED" -> ChargeCalculation.configured(fixedAmount);
            default -> ChargeCalculation.pending(
                    "COD_CHARGE_RULE_PENDING",
                    "COD additional charge rule is not configured yet");
        };
    }
}
