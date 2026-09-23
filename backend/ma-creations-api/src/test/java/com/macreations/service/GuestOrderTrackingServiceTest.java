package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.dto.GuestOrderTrackingResponse;
import com.macreations.entity.Customer;
import com.macreations.entity.Order;
import com.macreations.entity.OrderAddress;
import com.macreations.entity.OrderItem;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentStatus;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class GuestOrderTrackingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private GuestOrderTrackingService service;

    @BeforeEach
    void setUp() {
        service = new GuestOrderTrackingService(orderRepository, new OrderDtoMapper());
    }

    @Test
    void guestTrackSucceedsWithOrderNumberAndMatchingMobile() {
        Order order = guestOrder("MAC-G1", "9876543210");
        order.setTrackingNumber("AWB-55");
        when(orderRepository.findByOrderNumber("MAC-G1")).thenReturn(Optional.of(order));

        GuestOrderTrackingResponse response = service.track("MAC-G1", "9876543210");

        assertThat(response.getOrderNumber()).isEqualTo("MAC-G1");
        assertThat(response.getTrackingNumber()).isEqualTo("AWB-55");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getDeliveryCity()).isEqualTo("Mumbai");
    }

    @Test
    void guestTrackNormalizesMobileWithCountryCode() {
        Order order = guestOrder("MAC-G1", "9876543210");
        when(orderRepository.findByOrderNumber("MAC-G1")).thenReturn(Optional.of(order));

        GuestOrderTrackingResponse response = service.track("MAC-G1", "91 98765 43210");
        assertThat(response.getOrderNumber()).isEqualTo("MAC-G1");
    }

    @Test
    void wrongMobileReturnsNotFoundWithoutLeaking() {
        Order order = guestOrder("MAC-G1", "9876543210");
        when(orderRepository.findByOrderNumber("MAC-G1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.track("MAC-G1", "9123456780"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void registeredCustomerOrderNotAvailableViaGuestTrack() {
        Order order = guestOrder("MAC-C1", "9876543210");
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setMobileNumber("9876543210");
        order.setCustomer(customer);
        when(orderRepository.findByOrderNumber("MAC-C1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.track("MAC-C1", "9876543210"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void blankOrderNumberRejected() {
        assertThatThrownBy(() -> service.track("  ", "9876543210"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void blankMobileRejected() {
        assertThatThrownBy(() -> service.track("MAC-G1", " "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownOrderReturnsNotFound() {
        when(orderRepository.findByOrderNumber("MAC-NONE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.track("MAC-NONE", "9876543210"))
                .isInstanceOf(NotFoundException.class);
    }

    private Order guestOrder(String number, String mobile) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber(number);
        order.setCustomer(null);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.COD_PENDING);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setCurrency("INR");
        order.setItemsSubtotal(new BigDecimal("150.00"));
        order.setShippingCharge(new BigDecimal("20.00"));
        order.setCodCharge(new BigDecimal("20.00"));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setGrandTotal(new BigDecimal("190.00"));
        order.setContactName("Guest");
        order.setContactMobile(mobile);
        order.setGuestMobile(mobile);
        order.setPlacedAt(Instant.parse("2026-09-22T10:00:00Z"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductTitleSnapshot("Bottle");
        item.setQuantity(1);
        item.setUnitSellingPrice(new BigDecimal("150.00"));
        item.setLineSubtotal(new BigDecimal("150.00"));
        order.getItems().add(item);

        OrderAddress address = new OrderAddress();
        address.setOrder(order);
        address.setFullName("Guest");
        address.setMobile(mobile);
        address.setLine1("1 St");
        address.setCity("Mumbai");
        address.setState("MH");
        address.setPostalCode("400001");
        address.setCountry("IN");
        order.setShippingAddress(address);
        return order;
    }
}
