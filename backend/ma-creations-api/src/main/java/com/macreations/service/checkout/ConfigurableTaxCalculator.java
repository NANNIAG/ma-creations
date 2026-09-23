package com.macreations.service.checkout;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tax calculator.
 * V1 client rule: GST is not charged by this application; product prices are GST-inclusive.
 * Keep {@code taxAmount = 0} via ZERO mode. Do not invent a GST percentage.
 * Modes: ZERO | PENDING | FIXED (FIXED unused for V1).
 */
@Component
public class ConfigurableTaxCalculator implements TaxCalculator {

    private final String mode;
    private final BigDecimal fixedAmount;

    public ConfigurableTaxCalculator(
            @Value("${app.checkout.tax.mode:ZERO}") String mode,
            @Value("${app.checkout.tax.fixed-amount:0}") BigDecimal fixedAmount) {
        this.mode = mode == null ? "ZERO" : mode.trim().toUpperCase();
        this.fixedAmount = fixedAmount == null
                ? BigDecimal.ZERO.setScale(2)
                : fixedAmount.setScale(2);
    }

    @Override
    public ChargeCalculation calculate(BigDecimal itemsSubtotal, BigDecimal shippingCharge) {
        return switch (mode) {
            case "FIXED" -> ChargeCalculation.configured(fixedAmount);
            case "PENDING" -> ChargeCalculation.pending(
                    "TAX_RULE_PENDING",
                    "GST/tax rule is not configured yet");
            default -> ChargeCalculation.zero();
        };
    }
}
