package com.macreations.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutIssue {

    private String code;
    private String message;
    private Long productId;
    private BigDecimal previousPrice;
    private BigDecimal currentPrice;

    public CheckoutIssue() {
    }

    public CheckoutIssue(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static CheckoutIssue of(String code, String message) {
        return new CheckoutIssue(code, message);
    }

    public static CheckoutIssue forProduct(String code, String message, Long productId) {
        CheckoutIssue issue = new CheckoutIssue(code, message);
        issue.productId = productId;
        return issue;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getPreviousPrice() {
        return previousPrice;
    }

    public void setPreviousPrice(BigDecimal previousPrice) {
        this.previousPrice = previousPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}
