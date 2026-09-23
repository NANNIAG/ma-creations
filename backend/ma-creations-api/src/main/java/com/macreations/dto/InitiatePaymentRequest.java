package com.macreations.dto;

import jakarta.validation.constraints.NotBlank;

public class InitiatePaymentRequest {

    @NotBlank(message = "orderNumber is required")
    private String orderNumber;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
