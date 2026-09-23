package com.macreations.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AdminOrderDetailResponse {

    private String orderNumber;
    private Instant placedAt;
    private Long customerId;
    private String customerMobile;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String currency;
    private BigDecimal itemsSubtotal;
    private BigDecimal shippingCharge;
    private BigDecimal codCharge;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private String contactName;
    private String contactMobile;
    private String contactEmail;
    private String trackingNumber;
    private CustomerOrderDetailResponse.AddressSnapshot shippingAddress;
    private List<CustomerOrderDetailResponse.LineItem> items = new ArrayList<>();
    private List<PaymentTxSummary> paymentTransactions = new ArrayList<>();

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactMobile() {
        return contactMobile;
    }

    public void setContactMobile(String contactMobile) {
        this.contactMobile = contactMobile;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public CustomerOrderDetailResponse.AddressSnapshot getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(CustomerOrderDetailResponse.AddressSnapshot shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<CustomerOrderDetailResponse.LineItem> getItems() {
        return items;
    }

    public void setItems(List<CustomerOrderDetailResponse.LineItem> items) {
        this.items = items;
    }

    public List<PaymentTxSummary> getPaymentTransactions() {
        return paymentTransactions;
    }

    public void setPaymentTransactions(List<PaymentTxSummary> paymentTransactions) {
        this.paymentTransactions = paymentTransactions;
    }

    public static class PaymentTxSummary {
        private Long id;
        private String provider;
        private String providerOrderId;
        private String providerPaymentId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String failureCode;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getProviderOrderId() {
            return providerOrderId;
        }

        public void setProviderOrderId(String providerOrderId) {
            this.providerOrderId = providerOrderId;
        }

        public String getProviderPaymentId() {
            return providerPaymentId;
        }

        public void setProviderPaymentId(String providerPaymentId) {
            this.providerPaymentId = providerPaymentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getFailureCode() {
            return failureCode;
        }

        public void setFailureCode(String failureCode) {
            this.failureCode = failureCode;
        }
    }
}
