package com.macreations.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderSnapshotModelTest {

    @Test
    void orderItemKeepsIndependentPriceSnapshot() {
        Product product = new Product();
        product.setId(5L);
        product.setTitle("Live Title");
        product.setSellingPrice(new BigDecimal("999.00"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductTitleSnapshot("Sold As Bottle");
        item.setUnitSellingPrice(new BigDecimal("150.00"));
        item.setUnitMrpSnapshot(new BigDecimal("200.00"));
        item.setQuantity(2);
        item.setLineSubtotal(new BigDecimal("300.00"));

        product.setTitle("Renamed Later");
        product.setSellingPrice(new BigDecimal("50.00"));

        assertThat(item.getProductTitleSnapshot()).isEqualTo("Sold As Bottle");
        assertThat(item.getUnitSellingPrice()).isEqualByComparingTo("150.00");
        assertThat(item.getLineSubtotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void guestOrderHasNullCustomer() {
        Order order = new Order();
        order.setOrderNumber("MAC-20260922-ABCDEFGHJKLM");
        order.setCustomer(null);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.COD_PENDING);
        order.setPaymentMethod(PaymentMethod.COD);

        assertThat(order.getCustomer()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COD_PENDING);
    }

    @Test
    void orderAddressIsOrderBoundSnapshot() {
        Order order = new Order();
        OrderAddress address = new OrderAddress();
        address.setOrder(order);
        address.setFullName("Test User");
        address.setMobile("9876543210");
        address.setLine1("12 Street");
        address.setCity("Lucknow");
        address.setState("UP");
        address.setPostalCode("226001");
        address.setCountry("India");
        address.setLandmark("Near park");

        order.setShippingAddress(address);

        assertThat(order.getShippingAddress().getLandmark()).isEqualTo("Near park");
        assertThat(order.getShippingAddress().getOrder()).isSameAs(order);
    }
}
