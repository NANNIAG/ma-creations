package com.macreations.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutPreviewResponse {

    private boolean valid;
    private boolean readyToPlace;
    private boolean requiresReview;
    private String previewHash;
    private String currency = "INR";
    private PaymentMethod paymentMethod;
    private OrderStatus resultingOrderStatus;
    private PaymentStatus resultingPaymentStatus;

    private BigDecimal itemsSubtotal;
    private BigDecimal shippingCharge;
    private BigDecimal codCharge;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;

    private List<String> pendingRules = new ArrayList<>();
    private List<CheckoutLinePreview> lines = new ArrayList<>();
    private List<CheckoutIssue> issues = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isReadyToPlace() {
        return readyToPlace;
    }

    public void setReadyToPlace(boolean readyToPlace) {
        this.readyToPlace = readyToPlace;
    }

    public boolean isRequiresReview() {
        return requiresReview;
    }

    public void setRequiresReview(boolean requiresReview) {
        this.requiresReview = requiresReview;
    }

    public String getPreviewHash() {
        return previewHash;
    }

    public void setPreviewHash(String previewHash) {
        this.previewHash = previewHash;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OrderStatus getResultingOrderStatus() {
        return resultingOrderStatus;
    }

    public void setResultingOrderStatus(OrderStatus resultingOrderStatus) {
        this.resultingOrderStatus = resultingOrderStatus;
    }

    public PaymentStatus getResultingPaymentStatus() {
        return resultingPaymentStatus;
    }

    public void setResultingPaymentStatus(PaymentStatus resultingPaymentStatus) {
        this.resultingPaymentStatus = resultingPaymentStatus;
    }

    public BigDecimal getItemsSubtotal() {
        return itemsSubtotal;
    }

    public void setItemsSubtotal(BigDecimal itemsSubtotal) {
        this.itemsSubtotal = itemsSubtotal;
    }

    public BigDecimal getShippingCharge() {
        return shippingCharge;
    }

    public void setShippingCharge(BigDecimal shippingCharge) {
        this.shippingCharge = shippingCharge;
    }

    public BigDecimal getCodCharge() {
        return codCharge;
    }

    public void setCodCharge(BigDecimal codCharge) {
        this.codCharge = codCharge;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public List<String> getPendingRules() {
        return pendingRules;
    }

    public void setPendingRules(List<String> pendingRules) {
        this.pendingRules = pendingRules;
    }

    public List<CheckoutLinePreview> getLines() {
        return lines;
    }

    public void setLines(List<CheckoutLinePreview> lines) {
        this.lines = lines;
    }

    public List<CheckoutIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<CheckoutIssue> issues) {
        this.issues = issues;
    }
}
