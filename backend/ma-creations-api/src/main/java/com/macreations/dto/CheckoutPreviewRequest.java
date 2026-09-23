package com.macreations.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.macreations.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

/**
 * Checkout preview input. Must not include customer_id or any monetary totals.
 * {@link #lastSeenPrices} is optional and used only for price-change detection (not for charging).
 */
public class CheckoutPreviewRequest {

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    /**
     * Optional map of productId → unit selling price last shown to the shopper.
     * Compared to current Product.sellingPrice; never used as charge amount.
     */
    private Map<Long, BigDecimal> lastSeenPrices = new HashMap<>();

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Map<Long, BigDecimal> getLastSeenPrices() {
        return lastSeenPrices;
    }

    public void setLastSeenPrices(Map<Long, BigDecimal> lastSeenPrices) {
        this.lastSeenPrices = lastSeenPrices == null ? new HashMap<>() : lastSeenPrices;
    }
}
