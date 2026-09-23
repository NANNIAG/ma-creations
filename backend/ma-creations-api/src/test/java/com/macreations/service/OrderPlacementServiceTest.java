package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

import com.macreations.dto.CheckoutLinePreview;
import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.dto.InitiatePaymentResponse;
import com.macreations.dto.PlaceOrderRequest;
import com.macreations.dto.PlaceOrderResponse;
import com.macreations.entity.AdminUser;
import com.macreations.entity.Cart;
import com.macreations.entity.CartItem;
import com.macreations.entity.CartStatus;
import com.macreations.entity.Customer;
import com.macreations.entity.Order;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.Product;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.ForbiddenException;
import com.macreations.repository.CustomerRepository;
import com.macreations.repository.OrderRepository;
import com.macreations.repository.ProductRepository;
import com.macreations.security.AdminUserDetails;
import com.macreations.security.CustomerUserDetails;
import com.macreations.service.checkout.CheckoutService;
import com.macreations.service.payment.PaymentService;
import com.macreations.service.payment.razorpay.RazorpayProperties;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceTest {

    @Mock
    private CheckoutService checkoutService;
    @Mock
    private CartService cartService;
    @Mock
    private OrderService orderService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentService paymentService;

    private RazorpayProperties razorpayProperties;
    private OrderPlacementService orderPlacementService;

    @BeforeEach
    void setUp() {
        razorpayProperties = new RazorpayProperties();
        razorpayProperties.setPayLaterEnabled(false);
        orderPlacementService = new OrderPlacementService(
                checkoutService,
                cartService,
                orderService,
                orderRepository,
                customerRepository,
                productRepository,
                paymentService,
                razorpayProperties);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guestCodOrderCreatesSnapshotsAndClearsCart() {
        Product product = product(5L, "Bottle", "150.00");
        Cart cart = guestCart(11L, "g-tok", product, 2);
        when(orderRepository.findByIdempotencyKey("idem-cod-1")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(null), eq("g-tok"), any())).thenReturn(readyPreview(PaymentMethod.COD, "hash-1"));
        when(cartService.findActiveCartEntity(null, "g-tok")).thenReturn(Optional.of(cart));
        when(orderService.allocateOrderNumber()).thenReturn("MAC-1001");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceOrderResponse response = orderPlacementService.placeOrder("g-tok", placeRequest(PaymentMethod.COD, "hash-1", "idem-cod-1"));

        assertThat(response.getOrderNumber()).isEqualTo("MAC-1001");
        assertThat(response.getStatus()).isEqualTo("PLACED");
        assertThat(response.getPaymentStatus()).isEqualTo("COD_PENDING");
        assertThat(response.isRequiresOnlinePayment()).isFalse();
        assertThat(response.getRazorpay()).isNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("Bottle");
        assertThat(response.getItems().get(0).getUnitSellingPrice()).isEqualByComparingTo("150.00");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getCustomer()).isNull();
        assertThat(saved.getGuestMobile()).isEqualTo("9876543210");
        assertThat(saved.getShippingAddress().getCity()).isEqualTo("Mumbai");
        assertThat(saved.getItems()).hasSize(1);
        verify(cartService).clearCartById(11L);
        verify(paymentService, never()).initiate(any());
    }

    @Test
    void customerCodOrderSetsCustomerFromJwt() {
        Customer customer = new Customer();
        customer.setId(7L);
        authenticateCustomer(customer);
        Product product = product(5L, "Bottle", "100.00");
        Cart cart = customerCart(22L, customer, product, 1);
        when(orderRepository.findByIdempotencyKey("idem-cust-cod")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(7L), any(), any())).thenReturn(readyPreview(PaymentMethod.COD, "hash-c"));
        when(cartService.findActiveCartEntity(7L, null)).thenReturn(Optional.of(cart));
        when(orderService.allocateOrderNumber()).thenReturn("MAC-2002");
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceOrderResponse response = orderPlacementService.placeOrder(null, placeRequest(PaymentMethod.COD, "hash-c", "idem-cust-cod"));

        assertThat(response.getOrderNumber()).isEqualTo("MAC-2002");
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getCustomer().getId()).isEqualTo(7L);
        assertThat(orderCaptor.getValue().getGuestMobile()).isNull();
    }

    @Test
    void guestPrepaidOrderInitiatesRazorpayAndKeepsCart() {
        Product product = product(5L, "Bottle", "150.00");
        Cart cart = guestCart(11L, "g-tok", product, 1);
        when(orderRepository.findByIdempotencyKey("idem-upi")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(null), eq("g-tok"), any())).thenReturn(readyPreview(PaymentMethod.UPI, "hash-u"));
        when(cartService.findActiveCartEntity(null, "g-tok")).thenReturn(Optional.of(cart));
        when(orderService.allocateOrderNumber()).thenReturn("MAC-3003");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        InitiatePaymentResponse init = new InitiatePaymentResponse();
        init.setKeyId("rzp_test");
        init.setRazorpayOrderId("order_abc");
        init.setAmount(15000L);
        init.setCurrency("INR");
        when(paymentService.initiate(any())).thenReturn(init);

        PlaceOrderResponse response = orderPlacementService.placeOrder("g-tok", placeRequest(PaymentMethod.UPI, "hash-u", "idem-upi"));

        assertThat(response.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.isRequiresOnlinePayment()).isTrue();
        assertThat(response.getRazorpay().getRazorpayOrderId()).isEqualTo("order_abc");
        verify(cartService, never()).clearCartById(any());
        verify(paymentService).initiate(any());
    }

    @Test
    void customerCardOrderUsesServerAmountViaPaymentService() {
        Customer customer = new Customer();
        customer.setId(7L);
        authenticateCustomer(customer);
        Product product = product(5L, "Bottle", "200.00");
        Cart cart = customerCart(22L, customer, product, 1);
        when(orderRepository.findByIdempotencyKey("idem-card")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(7L), any(), any())).thenReturn(readyPreview(PaymentMethod.CARD, "hash-card"));
        when(cartService.findActiveCartEntity(7L, null)).thenReturn(Optional.of(cart));
        when(orderService.allocateOrderNumber()).thenReturn("MAC-4004");
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentService.initiate(any())).thenReturn(new InitiatePaymentResponse());

        PlaceOrderResponse response = orderPlacementService.placeOrder(null, placeRequest(PaymentMethod.CARD, "hash-card", "idem-card"));

        assertThat(response.getPaymentMethod()).isEqualTo("CARD");
        verify(paymentService).initiate(any());
    }

    @Test
    void netBankingOrderInitiatesPayment() {
        Product product = product(5L, "Bottle", "150.00");
        Cart cart = guestCart(11L, "g-tok", product, 1);
        when(orderRepository.findByIdempotencyKey("idem-nb")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(null), eq("g-tok"), any())).thenReturn(readyPreview(PaymentMethod.NET_BANKING, "hash-nb"));
        when(cartService.findActiveCartEntity(null, "g-tok")).thenReturn(Optional.of(cart));
        when(orderService.allocateOrderNumber()).thenReturn("MAC-5005");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentService.initiate(any())).thenReturn(new InitiatePaymentResponse());

        PlaceOrderResponse response = orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.NET_BANKING, "hash-nb", "idem-nb"));

        assertThat(response.getPaymentMethod()).isEqualTo("NET_BANKING");
        verify(paymentService).initiate(any());
    }

    @Test
    void payLaterDisabledReturnsStructuredError() {
        when(orderRepository.findByIdempotencyKey("idem-pl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.PAY_LATER, "hash", "idem-pl")))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("PAY_LATER_UNAVAILABLE");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void payLaterEnabledCreatesPrepaidOrder() {
        razorpayProperties.setPayLaterEnabled(true);
        Product product = product(5L, "Bottle", "150.00");
        Cart cart = guestCart(11L, "g-tok", product, 1);
        when(orderRepository.findByIdempotencyKey("idem-pl2")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(null), eq("g-tok"), any())).thenReturn(readyPreview(PaymentMethod.PAY_LATER, "hash-pl"));
        when(cartService.findActiveCartEntity(null, "g-tok")).thenReturn(Optional.of(cart));
        when(orderService.allocateOrderNumber()).thenReturn("MAC-6006");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentService.initiate(any())).thenReturn(new InitiatePaymentResponse());

        PlaceOrderResponse response = orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.PAY_LATER, "hash-pl", "idem-pl2"));

        assertThat(response.getPaymentMethod()).isEqualTo("PAY_LATER");
        verify(paymentService).initiate(any());
    }

    @Test
    void stalePreviewHashRequiresReview() {
        when(orderRepository.findByIdempotencyKey("idem-stale")).thenReturn(Optional.empty());
        when(checkoutService.preview(eq(null), eq("g-tok"), any())).thenReturn(readyPreview(PaymentMethod.COD, "fresh-hash"));

        assertThatThrownBy(() -> orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.COD, "old-hash", "idem-stale")))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("CHECKOUT_REVIEW_REQUIRED");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void emptyCartRejected() {
        when(orderRepository.findByIdempotencyKey("idem-empty")).thenReturn(Optional.empty());
        CheckoutPreviewResponse preview = new CheckoutPreviewResponse();
        preview.setValid(false);
        preview.setReadyToPlace(false);
        when(checkoutService.preview(eq(null), eq("g-tok"), any())).thenReturn(preview);

        assertThatThrownBy(() -> orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.COD, "hash", "idem-empty")))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("CHECKOUT_REVIEW_REQUIRED");
    }

    @Test
    void duplicateIdempotencyKeyDoesNotCreateSecondOrder() {
        Order existing = new Order();
        existing.setOrderNumber("MAC-EXIST");
        existing.setStatus(OrderStatus.PLACED);
        existing.setPaymentStatus(PaymentStatus.COD_PENDING);
        existing.setPaymentMethod(PaymentMethod.COD);
        existing.setCurrency("INR");
        existing.setItemsSubtotal(new BigDecimal("150.00"));
        existing.setShippingCharge(BigDecimal.ZERO);
        existing.setCodCharge(BigDecimal.ZERO);
        existing.setTaxAmount(BigDecimal.ZERO);
        existing.setDiscountAmount(BigDecimal.ZERO);
        existing.setGrandTotal(new BigDecimal("150.00"));
        existing.setItems(new ArrayList<>());
        when(orderRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));

        PlaceOrderResponse response = orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.COD, "hash", "idem-dup"));

        assertThat(response.getOrderNumber()).isEqualTo("MAC-EXIST");
        verify(orderRepository, never()).save(any());
        verify(orderService, never()).allocateOrderNumber();
    }

    @Test
    void adminCannotPlaceCustomerOrder() {
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setLoginIdentifier("admin@macreations.test");
        admin.setPasswordHash("x");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AdminUserDetails(admin), null, List.of()));

        assertThatThrownBy(() -> orderPlacementService.placeOrder(
                "g-tok", placeRequest(PaymentMethod.COD, "hash", "idem-admin")))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code")
                .isEqualTo("ADMIN_NOT_CUSTOMER");
    }

    @Test
    void customerCannotReuseAnotherCustomersIdempotencyKey() {
        Customer owner = new Customer();
        owner.setId(1L);
        Order existing = new Order();
        existing.setOrderNumber("MAC-OTHER");
        existing.setCustomer(owner);
        existing.setStatus(OrderStatus.PLACED);
        existing.setPaymentStatus(PaymentStatus.COD_PENDING);
        existing.setPaymentMethod(PaymentMethod.COD);
        existing.setItems(new ArrayList<>());
        when(orderRepository.findByIdempotencyKey("idem-own")).thenReturn(Optional.of(existing));

        Customer other = new Customer();
        other.setId(2L);
        authenticateCustomer(other);

        assertThatThrownBy(() -> orderPlacementService.placeOrder(
                null, placeRequest(PaymentMethod.COD, "hash", "idem-own")))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code")
                .isEqualTo("ORDER_ACCESS_DENIED");
    }

    private void authenticateCustomer(Customer customer) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CustomerUserDetails(customer), null, List.of()));
    }

    private PlaceOrderRequest placeRequest(PaymentMethod method, String hash, String idem) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setPaymentMethod(method);
        request.setPreviewHash(hash);
        request.setIdempotencyKey(idem);
        request.setContactName("Muskan");
        request.setContactMobile("9876543210");
        request.setContactEmail("guest@example.com");
        PlaceOrderRequest.ShippingAddressRequest ship = new PlaceOrderRequest.ShippingAddressRequest();
        ship.setFullName("Muskan");
        ship.setMobile("9876543210");
        ship.setLine1("12 Flower Street");
        ship.setCity("Mumbai");
        ship.setState("MH");
        ship.setPostalCode("400001");
        ship.setCountry("India");
        request.setShippingAddress(ship);
        return request;
    }

    private CheckoutPreviewResponse readyPreview(PaymentMethod method, String hash) {
        CheckoutPreviewResponse preview = new CheckoutPreviewResponse();
        preview.setValid(true);
        preview.setReadyToPlace(true);
        preview.setRequiresReview(false);
        preview.setPreviewHash(hash);
        preview.setCurrency("INR");
        preview.setPaymentMethod(method);
        preview.setItemsSubtotal(new BigDecimal("150.00"));
        preview.setShippingCharge(BigDecimal.ZERO);
        preview.setCodCharge(BigDecimal.ZERO);
        preview.setTaxAmount(BigDecimal.ZERO);
        preview.setDiscountAmount(BigDecimal.ZERO);
        preview.setGrandTotal(new BigDecimal("150.00"));
        CheckoutLinePreview line = new CheckoutLinePreview();
        line.setProductId(5L);
        line.setTitle("Bottle");
        line.setSlug("bottle");
        line.setQuantity(method == PaymentMethod.COD ? 2 : 1);
        line.setUnitSellingPrice(new BigDecimal("150.00"));
        line.setLineSubtotal(new BigDecimal(method == PaymentMethod.COD ? "300.00" : "150.00"));
        if (method == PaymentMethod.COD) {
            preview.setItemsSubtotal(new BigDecimal("300.00"));
            preview.setGrandTotal(new BigDecimal("300.00"));
        }
        preview.setLines(List.of(line));
        return preview;
    }

    private Product product(Long id, String title, String price) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setSlug(title.toLowerCase());
        product.setSellingPrice(new BigDecimal(price));
        product.setPublished(true);
        return product;
    }

    private Cart guestCart(Long id, String token, Product product, int qty) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setGuestToken(token);
        cart.setStatus(CartStatus.ACTIVE);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(qty);
        cart.setItems(new ArrayList<>(List.of(item)));
        return cart;
    }

    private Cart customerCart(Long id, Customer customer, Product product, int qty) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setCustomer(customer);
        cart.setStatus(CartStatus.ACTIVE);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(qty);
        cart.setItems(new ArrayList<>(List.of(item)));
        return cart;
    }
}
