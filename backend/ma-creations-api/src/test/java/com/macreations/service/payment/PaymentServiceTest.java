package com.macreations.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.macreations.dto.InitiatePaymentRequest;
import com.macreations.dto.InitiatePaymentResponse;
import com.macreations.dto.VerifyPaymentRequest;
import com.macreations.entity.AdminUser;
import com.macreations.entity.Customer;
import com.macreations.entity.Order;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentProvider;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.PaymentTransaction;
import com.macreations.entity.PaymentTransactionStatus;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.ForbiddenException;
import com.macreations.repository.OrderRepository;
import com.macreations.repository.PaymentTransactionRepository;
import com.macreations.security.AdminUserDetails;
import com.macreations.security.CustomerUserDetails;
import com.macreations.service.CartService;
import com.macreations.service.payment.PaymentGateway.CreatePaymentCommand;
import com.macreations.service.payment.PaymentGateway.CreatePaymentResult;
import com.macreations.service.payment.PaymentGateway.ParsedWebhookEvent;
import com.macreations.service.payment.PaymentGateway.VerifyPaymentCommand;
import com.macreations.service.payment.razorpay.RazorpayProperties;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private CartService cartService;

    private RazorpayProperties razorpayProperties;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        razorpayProperties = new RazorpayProperties();
        razorpayProperties.setKeyId("rzp_test_key");
        razorpayProperties.setKeySecret("secret");
        razorpayProperties.setWebhookSecret("whsec");
        razorpayProperties.setPayLaterEnabled(false);
        paymentService = new PaymentService(
                orderRepository, paymentTransactionRepository, paymentGateway, razorpayProperties, cartService);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void initiateUsesServerOrderAmountAndCreatesProviderOrder() {
        Order order = payableOrder(PaymentMethod.UPI, null);
        when(orderRepository.findByOrderNumber("MAC-1")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> {
            PaymentTransaction tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(10L);
            }
            return tx;
        });
        when(paymentGateway.createPayment(any())).thenReturn(
                new CreatePaymentResult("order_Rzp1", 56700L, "INR", "order_Rzp1"));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderNumber("MAC-1");
        request.setIdempotencyKey("idem-1");

        InitiatePaymentResponse response = paymentService.initiate(request);

        ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);
        verify(paymentGateway).createPayment(captor.capture());
        assertThat(captor.getValue().amountInr()).isEqualByComparingTo("567.00");
        assertThat(response.getAmount()).isEqualTo(56700L);
        assertThat(response.getKeyId()).isEqualTo("rzp_test_key");
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_Rzp1");
        assertThat(response.getOrderNumber()).isEqualTo("MAC-1");
    }

    @Test
    void initiateRejectsCod() {
        Order order = payableOrder(PaymentMethod.COD, null);
        order.setPaymentStatus(PaymentStatus.COD_PENDING);
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findByOrderNumber("MAC-COD")).thenReturn(Optional.of(order));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderNumber("MAC-COD");
        request.setIdempotencyKey("idem-cod");

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("COD_NOT_ONLINE");
        verify(paymentGateway, never()).createPayment(any());
    }

    @Test
    void initiateRejectsPayLaterWhenNotEnabled() {
        Order order = payableOrder(PaymentMethod.PAY_LATER, null);
        when(orderRepository.findByOrderNumber("MAC-PL")).thenReturn(Optional.of(order));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderNumber("MAC-PL");
        request.setIdempotencyKey("idem-pl");

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("PAY_LATER_UNAVAILABLE");
    }

    @Test
    void customerCannotPayAnotherCustomersOrder() {
        Customer owner = customer(1L);
        Customer other = customer(2L);
        Order order = payableOrder(PaymentMethod.CARD, owner);
        authenticateCustomer(other);
        when(orderRepository.findByOrderNumber("MAC-2")).thenReturn(Optional.of(order));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderNumber("MAC-2");
        request.setIdempotencyKey("idem-2");

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code")
                .isEqualTo("ORDER_ACCESS_DENIED");
    }

    @Test
    void adminCannotInitiatePayment() {
        authenticateAdmin();

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderNumber("MAC-3");
        request.setIdempotencyKey("idem-3");

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code")
                .isEqualTo("ADMIN_NOT_CUSTOMER");
    }

    @Test
    void verifyMarksOrderPaidWhenSignatureValid() {
        Order order = payableOrder(PaymentMethod.NET_BANKING, null);
        PaymentTransaction tx = pendingTx(order, "order_Rzp2");
        when(orderRepository.findByOrderNumber("MAC-1")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, "order_Rzp2"))
                .thenReturn(Optional.of(tx));
        when(paymentGateway.verifyCheckoutSignature(any(VerifyPaymentCommand.class))).thenReturn(true);

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setOrderNumber("MAC-1");
        request.setRazorpayOrderId("order_Rzp2");
        request.setRazorpayPaymentId("pay_1");
        request.setRazorpaySignature("sig");

        var response = paymentService.verify(request);
        assertThat(response.isPaid()).isTrue();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.CAPTURED);
    }

    @Test
    void webhookDuplicateEventIsIgnored() {
        Order order = payableOrder(PaymentMethod.UPI, null);
        PaymentTransaction tx = pendingTx(order, "order_Rzp3");
        tx.setStatus(PaymentTransactionStatus.CAPTURED);
        tx.setLastWebhookEventId("evt_1");
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PLACED);

        when(paymentGateway.verifyWebhookSignature("body", "sig")).thenReturn(true);
        when(paymentGateway.parseWebhookEvent("body")).thenReturn(
                new ParsedWebhookEvent("evt_1", "payment.captured", "order_Rzp3", "pay_9", "captured", "upi"));
        when(paymentTransactionRepository.findByLastWebhookEventId("evt_1")).thenReturn(Optional.of(tx));

        paymentService.handleRazorpayWebhook("body", "sig");
        verify(paymentTransactionRepository, never()).findByProviderAndProviderOrderId(any(), any());
    }

    @Test
    void webhookInvalidSignatureRejected() {
        when(paymentGateway.verifyWebhookSignature("body", "bad")).thenReturn(false);
        assertThatThrownBy(() -> paymentService.handleRazorpayWebhook("body", "bad"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code")
                .isEqualTo("INVALID_WEBHOOK_SIGNATURE");
    }

    @Test
    void webhookCapturedUpdatesPaymentAndOrder() {
        Order order = payableOrder(PaymentMethod.UPI, null);
        PaymentTransaction tx = pendingTx(order, "order_Rzp4");
        when(paymentGateway.verifyWebhookSignature("body", "sig")).thenReturn(true);
        when(paymentGateway.parseWebhookEvent("body")).thenReturn(
                new ParsedWebhookEvent("evt_2", "payment.captured", "order_Rzp4", "pay_2", "captured", "upi"));
        when(paymentTransactionRepository.findByLastWebhookEventId("evt_2")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByProviderAndProviderPaymentId(PaymentProvider.RAZORPAY, "pay_2"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, "order_Rzp4"))
                .thenReturn(Optional.of(tx));

        paymentService.handleRazorpayWebhook("body", "sig");

        assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.CAPTURED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void webhookFailedUpdatesStatuses() {
        Order order = payableOrder(PaymentMethod.CARD, null);
        PaymentTransaction tx = pendingTx(order, "order_Rzp5");
        when(paymentGateway.verifyWebhookSignature("body", "sig")).thenReturn(true);
        when(paymentGateway.parseWebhookEvent("body")).thenReturn(
                new ParsedWebhookEvent("evt_3", "payment.failed", "order_Rzp5", "pay_3", "failed", "card"));
        when(paymentTransactionRepository.findByLastWebhookEventId("evt_3")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByProviderAndProviderPaymentId(PaymentProvider.RAZORPAY, "pay_3"))
                .thenReturn(Optional.of(tx));

        paymentService.handleRazorpayWebhook("body", "sig");

        assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    private Order payableOrder(PaymentMethod method, Customer customer) {
        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber(method == PaymentMethod.COD ? "MAC-COD" : "MAC-1");
        if (method == PaymentMethod.PAY_LATER) {
            order.setOrderNumber("MAC-PL");
        }
        if (method == PaymentMethod.CARD && customer != null) {
            order.setOrderNumber("MAC-2");
        }
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(method);
        order.setCurrency("INR");
        order.setItemsSubtotal(new BigDecimal("567.00"));
        order.setShippingCharge(BigDecimal.ZERO);
        order.setCodCharge(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setGrandTotal(new BigDecimal("567.00"));
        order.setContactName("Test");
        order.setContactMobile("9999999999");
        order.setPlacedAt(Instant.now());
        return order;
    }

    private PaymentTransaction pendingTx(Order order, String providerOrderId) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(5L);
        tx.setOrder(order);
        tx.setProvider(PaymentProvider.RAZORPAY);
        tx.setProviderOrderId(providerOrderId);
        tx.setPaymentMethod(order.getPaymentMethod());
        tx.setAmount(order.getGrandTotal());
        tx.setCurrency("INR");
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setIdempotencyKey("idem-" + providerOrderId);
        return tx;
    }

    private Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setMobileNumber("9" + id + "000000000".substring(0, 9));
        return customer;
    }

    private void authenticateCustomer(Customer customer) {
        CustomerUserDetails details = new CustomerUserDetails(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private void authenticateAdmin() {
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setLoginIdentifier("admin@macreations.test");
        admin.setPasswordHash("x");
        AdminUserDetails details = new AdminUserDetails(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
