package com.macreations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateOrderTrackingRequest {

    @NotBlank(message = "trackingNumber is required")
    @Size(max = 64, message = "trackingNumber must be at most 64 characters")
    private String trackingNumber;

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}
