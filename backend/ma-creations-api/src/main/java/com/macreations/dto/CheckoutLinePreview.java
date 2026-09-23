package com.macreations.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutLinePreview {

    private Long productId;
    private String title;
    private String slug;
    private int quantity;
    private BigDecimal unitSellingPrice;
    private BigDecimal unitMrp;
    private BigDecimal lineSubtotal;
    private BigDecimal lastSeenUnitPrice;
    private boolean priceChanged;

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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitSellingPrice() {
        return unitSellingPrice;
    }

    public void setUnitSellingPrice(BigDecimal unitSellingPrice) {
        this.unitSellingPrice = unitSellingPrice;
    }

    public BigDecimal getUnitMrp() {
        return unitMrp;
    }

    public void setUnitMrp(BigDecimal unitMrp) {
        this.unitMrp = unitMrp;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public void setLineSubtotal(BigDecimal lineSubtotal) {
        this.lineSubtotal = lineSubtotal;
    }

    public BigDecimal getLastSeenUnitPrice() {
        return lastSeenUnitPrice;
    }

    public void setLastSeenUnitPrice(BigDecimal lastSeenUnitPrice) {
        this.lastSeenUnitPrice = lastSeenUnitPrice;
    }

    public boolean isPriceChanged() {
        return priceChanged;
    }

    public void setPriceChanged(boolean priceChanged) {
        this.priceChanged = priceChanged;
    }
}
