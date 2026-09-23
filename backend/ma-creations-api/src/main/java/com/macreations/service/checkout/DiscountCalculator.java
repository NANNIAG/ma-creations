package com.macreations.service.checkout;

import java.math.BigDecimal;

public interface DiscountCalculator {
    ChargeCalculation calculate(BigDecimal itemsSubtotal);
}
