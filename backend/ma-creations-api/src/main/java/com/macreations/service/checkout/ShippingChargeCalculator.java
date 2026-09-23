package com.macreations.service.checkout;

import java.math.BigDecimal;

import com.macreations.entity.PaymentMethod;

public interface ShippingChargeCalculator {
    ChargeCalculation calculate(BigDecimal itemsSubtotal, PaymentMethod paymentMethod);
}
