package com.macreations.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of POST /api/orders. For online methods, razorpay fields may be present for Checkout.
 */
public class PlaceOrderResponse {

    private String orderNumber;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private String currency;
    private BigDecimal itemsSubtotal;
    private BigDecimal shippingCharge;
    private BigDecimal codCharge;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private boolean requiresOnlinePayment;
    private InitiatePaymentResponse razorpay;
    private List<OrderLineSummary> items = new ArrayList<>();

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isRequiresOnlinePayment() {
        return requiresOnlinePayment;
    }

    public void setRequiresOnlinePayment(boolean requiresOnlinePayment) {
        this.requiresOnlinePayment = requiresOnlinePayment;
    }

    public InitiatePaymentResponse getRazorpay() {
        return razorpay;
    }

    public void setRazorpay(InitiatePaymentResponse razorpay) {
        this.razorpay = razorpay;
    }

    public List<OrderLineSummary> getItems() {
        return items;
    }

    public void setItems(List<OrderLineSummary> items) {
        this.items = items;
    }

    public static class OrderLineSummary {
        private Long productId;
        private String title;
        private Integer quantity;
        private BigDecimal unitSellingPrice;
        private BigDecimal lineSubtotal;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitSellingPrice() {
            return unitSellingPrice;
        }

        public void setUnitSellingPrice(BigDecimal unitSellingPrice) {
            this.unitSellingPrice = unitSellingPrice;
        }

        public BigDecimal getLineSubtotal() {
            return lineSubtotal;
        }

        public void setLineSubtotal(BigDecimal lineSubtotal) {
            this.lineSubtotal = lineSubtotal;
        }
    }
}
