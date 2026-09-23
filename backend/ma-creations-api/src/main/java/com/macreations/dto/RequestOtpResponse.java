package com.macreations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestOtpResponse {

    private String message;
    private Integer retryAfterSeconds;

    public RequestOtpResponse() {
    }

    public RequestOtpResponse(String message, Integer retryAfterSeconds) {
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Integer retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
