package com.macreations.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Confirmed CMS update fields only (title, category, selling price, MRP).
 * Optional replacement image is a separate multipart part.
 */
public class UpdateProductRequest {

    @NotBlank(message = "Product title is required")
    private String title;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Selling price must not be negative")
    private BigDecimal sellingPrice;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "MRP must not be negative")
    private BigDecimal mrp;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }
}
