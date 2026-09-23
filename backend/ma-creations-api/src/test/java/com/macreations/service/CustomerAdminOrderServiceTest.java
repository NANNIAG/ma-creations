package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.macreations.dto.AdminOrderDetailResponse;
import com.macreations.dto.CustomerOrderDetailResponse;
import com.macreations.dto.CustomerOrderListItemResponse;
import com.macreations.dto.PageResponse;
import com.macreations.dto.UpdateOrderStatusRequest;
import com.macreations.dto.UpdateOrderTrackingRequest;
import com.macreations.entity.Customer;
import com.macreations.entity.Order;
import com.macreations.entity.OrderAddress;
import com.macreations.entity.OrderItem;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.PaymentTransaction;
import com.macreations.entity.PaymentTransactionStatus;
import com.macreations.entity.PaymentProvider;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.OrderRepository;
import com.macreations.repository.PaymentTransactionRepository;
import com.macreations.security.CustomerUserDetails;

@ExtendWith(MockitoExtension.class)
class CustomerAdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    private OrderDtoMapper mapper;
    private OrderStatusTransitionValidator validator;
    private CustomerOrderService customerOrderService;
    private AdminOrderService adminOrderService;

    @BeforeEach
    void setUp() {
        mapper = new OrderDtoMapper();
        validator = new OrderStatusTransitionValidator();
        customerOrderService = new CustomerOrderService(orderRepository, mapper);
        adminOrderService = new AdminOrderService(
                orderRepository, paymentTransactionRepository, mapper, validator);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerListsOwnOrdersNewestFirstWithPagination() {
        authenticateCustomer(10L);
        Order newer = sampleOrder("MAC-2", 10L, Instant.parse("2026-09-22T10:00:00Z"));
        Order older = sampleOrder("MAC-1", 10L, Instant.parse("2026-09-21T10:00:00Z"));
        when(orderRepository.findByCustomerIdOrderByPlacedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newer, older), PageRequest.of(0, 20), 2));

        PageResponse<CustomerOrderListItemResponse> page = customerOrderService.listMyOrders(0, 20);

        assertThat(page.getContent()).extracting(CustomerOrderListItemResponse::getOrderNumber)
                .containsExactly("MAC-2", "MAC-1");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getItemCount()).isEqualTo(1);
    }

    @Test
    void customerCanOpenOwnOrderDetailWithSnapshots() {
        authenticateCustomer(10L);
        Order order = sampleOrder("MAC-9", 10L, Instant.parse("2026-09-22T10:00:00Z"));
        when(orderRepository.findByOrderNumber("MAC-9")).thenReturn(Optional.of(order));

        CustomerOrderDetailResponse detail = customerOrderService.getMyOrder("MAC-9");

        assertThat(detail.getOrderNumber()).isEqualTo("MAC-9");
        assertThat(detail.getItems()).hasSize(1);
        assertThat(detail.getItems().get(0).getTitle()).isEqualTo("Snap Bottle");
        assertThat(detail.getItems().get(0).getUnitSellingPrice()).isEqualByComparingTo("150.00");
        assertThat(detail.getShippingAddress().getPostalCode()).isEqualTo("400001");
        assertThat(detail.getTaxAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void customerCannotOpenAnotherCustomersOrder() {
        authenticateCustomer(10L);
        Order other = sampleOrder("MAC-other", 99L, Instant.now());
        when(orderRepository.findByOrderNumber("MAC-other")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> customerOrderService.getMyOrder("MAC-other"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void adminListsOrdersWithFilters() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        when(orderRepository.searchForAdmin(eq("98765"), eq(OrderStatus.PLACED), eq(PaymentStatus.COD_PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

        var page = adminOrderService.listOrders("98765", OrderStatus.PLACED, PaymentStatus.COD_PENDING, 0, 20);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getOrderNumber()).isEqualTo("MAC-A");
        assertThat(page.getContent().get(0).getContactMobile()).isEqualTo("9876543210");
    }

    @Test
    void adminGetsOrderDetailWithSafePaymentSummary() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(7L);
        tx.setProvider(PaymentProvider.RAZORPAY);
        tx.setProviderOrderId("order_x");
        tx.setProviderPaymentId("pay_y");
        tx.setStatus(PaymentTransactionStatus.CAPTURED);
        tx.setAmount(new BigDecimal("190.00"));
        tx.setCurrency("INR");
        tx.setPaymentMethod(PaymentMethod.UPI);
        when(orderRepository.findByOrderNumber("MAC-A")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()))
                .thenReturn(List.of(tx));

        AdminOrderDetailResponse detail = adminOrderService.getOrder("MAC-A");

        assertThat(detail.getCustomerId()).isEqualTo(10L);
        assertThat(detail.getPaymentTransactions()).hasSize(1);
        assertThat(detail.getPaymentTransactions().get(0).getProviderPaymentId()).isEqualTo("pay_y");
        assertThat(detail.getPaymentTransactions().get(0).getProviderOrderId()).isEqualTo("order_x");
    }

    @Test
    void adminStatusUpdateAllowsPlacedToProcessingAndPreservesPaymentStatus() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.COD_PENDING);
        when(orderRepository.findByOrderNumber("MAC-A")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.PROCESSING);

        AdminOrderDetailResponse updated = adminOrderService.updateStatus("MAC-A", request);

        assertThat(updated.getOrderStatus()).isEqualTo("PROCESSING");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COD_PENDING);
        assertThat(updated.getPaymentStatus()).isEqualTo("COD_PENDING");
        verify(orderRepository).save(order);
    }

    @Test
    void adminRejectsInvalidTransitionAndCancel() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findByOrderNumber("MAC-A")).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest skip = new UpdateOrderStatusRequest();
        skip.setStatus(OrderStatus.SHIPPED);
        assertThatThrownBy(() -> adminOrderService.updateStatus("MAC-A", skip))
                .isInstanceOf(BadRequestException.class);

        UpdateOrderStatusRequest cancel = new UpdateOrderStatusRequest();
        cancel.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> adminOrderService.updateStatus("MAC-A", cancel))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void adminRejectsPaymentDrivenStatuses() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findByOrderNumber("MAC-A")).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest pending = new UpdateOrderStatusRequest();
        pending.setStatus(OrderStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> adminOrderService.updateStatus("MAC-A", pending))
                .isInstanceOf(BadRequestException.class);

        UpdateOrderStatusRequest failed = new UpdateOrderStatusRequest();
        failed.setStatus(OrderStatus.PAYMENT_FAILED);
        assertThatThrownBy(() -> adminOrderService.updateStatus("MAC-A", failed))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adminCannotTransitionFromPendingPayment() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findByOrderNumber("MAC-A")).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> adminOrderService.updateStatus("MAC-A", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void customerPageSizeCapEnforced() {
        authenticateCustomer(10L);
        assertThatThrownBy(() -> customerOrderService.listMyOrders(0, 100))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adminSearchUsesNullQueryWhenBlank() {
        when(orderRepository.searchForAdmin(isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var page = adminOrderService.listOrders("  ", null, null, 0, 20);

        assertThat(page.getContent()).isEmpty();
        verify(orderRepository).searchForAdmin(isNull(), isNull(), isNull(), any());
    }

    @Test
    void adminCanAddAndUpdateTrackingWithoutChangingPaymentOrStatus() {
        Order order = sampleOrder("MAC-A", 10L, Instant.now());
        order.setStatus(OrderStatus.SHIPPED);
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findByOrderNumber("MAC-A")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        UpdateOrderTrackingRequest request = new UpdateOrderTrackingRequest();
        request.setTrackingNumber("  AWB-999  ");

        AdminOrderDetailResponse updated = adminOrderService.updateTracking("MAC-A", request);

        assertThat(updated.getTrackingNumber()).isEqualTo("AWB-999");
        assertThat(order.getTrackingNumber()).isEqualTo("AWB-999");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        request.setTrackingNumber("AWB-1000");
        AdminOrderDetailResponse again = adminOrderService.updateTracking("MAC-A", request);
        assertThat(again.getTrackingNumber()).isEqualTo("AWB-1000");
    }

    @Test
    void adminRejectsBlankTrackingWithoutSave() {
        UpdateOrderTrackingRequest blank = new UpdateOrderTrackingRequest();
        blank.setTrackingNumber("   ");
        assertThatThrownBy(() -> adminOrderService.updateTracking("MAC-A", blank))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void adminRejectsOversizedTracking() {
        UpdateOrderTrackingRequest tooLong = new UpdateOrderTrackingRequest();
        tooLong.setTrackingNumber("x".repeat(65));
        assertThatThrownBy(() -> adminOrderService.updateTracking("MAC-A", tooLong))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void customerDetailIncludesTrackingNumber() {
        authenticateCustomer(10L);
        Order order = sampleOrder("MAC-9", 10L, Instant.now());
        order.setTrackingNumber("TRACK-1");
        when(orderRepository.findByOrderNumber("MAC-9")).thenReturn(Optional.of(order));

        CustomerOrderDetailResponse detail = customerOrderService.getMyOrder("MAC-9");
        assertThat(detail.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    private void authenticateCustomer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setMobileNumber("9000000000");
        customer.setEnabled(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CustomerUserDetails(customer), null, List.of()));
    }

    private Order sampleOrder(String number, Long customerId, Instant placedAt) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setMobileNumber("9876543210");
        customer.setEnabled(true);

        Order order = new Order();
        order.setId(Math.abs(number.hashCode() * 1L));
        order.setOrderNumber(number);
        order.setCustomer(customer);
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
        order.setContactName("Test User");
        order.setContactMobile("9876543210");
        order.setPlacedAt(placedAt);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductTitleSnapshot("Snap Bottle");
        item.setQuantity(1);
        item.setUnitSellingPrice(new BigDecimal("150.00"));
        item.setUnitMrpSnapshot(new BigDecimal("200.00"));
        item.setLineSubtotal(new BigDecimal("150.00"));
        order.getItems().add(item);

        OrderAddress address = new OrderAddress();
        address.setOrder(order);
        address.setFullName("Test User");
        address.setMobile("9876543210");
        address.setLine1("1 Main St");
        address.setCity("Mumbai");
        address.setState("MH");
        address.setPostalCode("400001");
        address.setCountry("IN");
        order.setShippingAddress(address);
        return order;
    }
}
