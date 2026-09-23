package com.macreations.dto;

import jakarta.validation.constraints.NotBlank;

public class RequestOtpRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}
