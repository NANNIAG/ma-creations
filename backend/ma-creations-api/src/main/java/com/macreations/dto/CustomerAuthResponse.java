package com.macreations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerAuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresInSeconds;
    private CustomerResponse customer;

    public CustomerAuthResponse() {
    }

    public CustomerAuthResponse(String accessToken, Long expiresInSeconds, CustomerResponse customer) {
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
        this.customer = customer;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public CustomerResponse getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerResponse customer) {
        this.customer = customer;
    }
}
