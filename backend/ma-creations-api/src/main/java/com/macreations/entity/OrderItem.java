package com.macreations.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "order_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_item_order_product", columnNames = {"order_id", "product_id"})
        }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_title_snapshot", nullable = false)
    private String productTitleSnapshot;

    @Column(name = "product_slug_snapshot")
    private String productSlugSnapshot;

    @Column(name = "unit_selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitSellingPrice;

    @Column(name = "unit_mrp_snapshot", precision = 12, scale = 2)
    private BigDecimal unitMrpSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineSubtotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getProductTitleSnapshot() {
        return productTitleSnapshot;
    }

    public void setProductTitleSnapshot(String productTitleSnapshot) {
        this.productTitleSnapshot = productTitleSnapshot;
    }

    public String getProductSlugSnapshot() {
        return productSlugSnapshot;
    }

    public void setProductSlugSnapshot(String productSlugSnapshot) {
        this.productSlugSnapshot = productSlugSnapshot;
    }

    public BigDecimal getUnitSellingPrice() {
        return unitSellingPrice;
    }

    public void setUnitSellingPrice(BigDecimal unitSellingPrice) {
        this.unitSellingPrice = unitSellingPrice;
    }

    public BigDecimal getUnitMrpSnapshot() {
        return unitMrpSnapshot;
    }

    public void setUnitMrpSnapshot(BigDecimal unitMrpSnapshot) {
        this.unitMrpSnapshot = unitMrpSnapshot;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public void setLineSubtotal(BigDecimal lineSubtotal) {
        this.lineSubtotal = lineSubtotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
