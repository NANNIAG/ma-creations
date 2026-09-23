package com.macreations.service.checkout;

import java.math.BigDecimal;

public interface TaxCalculator {
    ChargeCalculation calculate(BigDecimal itemsSubtotal, BigDecimal shippingCharge);
}
