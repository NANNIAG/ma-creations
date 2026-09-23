package com.macreations.dto;

import jakarta.validation.constraints.NotBlank;

public class ReportPaymentFailureRequest {

    @NotBlank(message = "orderNumber is required")
    private String orderNumber;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}
