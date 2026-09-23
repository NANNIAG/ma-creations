package com.macreations.service.checkout;

import java.math.BigDecimal;

import com.macreations.entity.PaymentMethod;

public interface CodChargeCalculator {
    /**
     * Returns COD surcharge. For non-COD methods, returns configured zero.
     * When COD is selected and the client rule is pending, returns unconfigured.
     */
    ChargeCalculation calculate(BigDecimal itemsSubtotal, PaymentMethod paymentMethod);
}
