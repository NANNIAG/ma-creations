package com.macreations.service.checkout;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * No discount rules exist yet — always zero.
 */
@Component
public class ZeroDiscountCalculator implements DiscountCalculator {

    @Override
    public ChargeCalculation calculate(BigDecimal itemsSubtotal) {
        return ChargeCalculation.zero();
    }
}
