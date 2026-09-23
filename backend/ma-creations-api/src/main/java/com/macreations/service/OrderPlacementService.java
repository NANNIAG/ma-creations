package com.macreations.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.macreations.dto.CheckoutPreviewRequest;
import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.dto.InitiatePaymentRequest;
import com.macreations.dto.InitiatePaymentResponse;
import com.macreations.dto.OrderSummaryResponse;
import com.macreations.dto.PlaceOrderRequest;
import com.macreations.dto.PlaceOrderResponse;
import com.macreations.entity.Cart;
import com.macreations.entity.Customer;
import com.macreations.entity.Order;
import com.macreations.entity.OrderAddress;
import com.macreations.entity.OrderItem;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.Product;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.ForbiddenException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.CustomerRepository;
import com.macreations.repository.OrderRepository;
import com.macreations.repository.ProductRepository;
import com.macreations.security.AdminUserDetails;
import com.macreations.security.SecurityUtils;
import com.macreations.service.checkout.CheckoutService;
import com.macreations.service.payment.PaymentService;
import com.macreations.service.payment.razorpay.RazorpayProperties;
import com.macreations.util.MobileNumberUtils;

@Service
public class OrderPlacementService {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacementService.class);
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUND = RoundingMode.HALF_UP;
    private static final Set<PaymentMethod> ONLINE_METHODS = Set.of(
            PaymentMethod.UPI,
            PaymentMethod.CARD,
            PaymentMethod.NET_BANKING,
            PaymentMethod.PAY_LATER);

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final RazorpayProperties razorpayProperties;

    public OrderPlacementService(
            CheckoutService checkoutService,
            CartService cartService,
            OrderService orderService,
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            PaymentService paymentService,
            RazorpayProperties razorpayProperties) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.razorpayProperties = razorpayProperties;
    }

    @Transactional
    public PlaceOrderResponse placeOrder(String guestToken, PlaceOrderRequest request) {
        rejectAdminActingAsCustomer();

        Long customerId = SecurityUtils.currentCustomerId().orElse(null);
        if (customerId == null && !StringUtils.hasText(guestToken)) {
            throw new BadRequestException("CART_TOKEN_REQUIRED", "Cart token is required for guest checkout");
        }

        String idempotencyKey = request.getIdempotencyKey().trim();
        Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Order prior = existing.get();
            assertIdempotentOrderAccess(prior, customerId);
            return toPlaceResponse(prior, maybeReinitiatePayment(prior, idempotencyKey));
        }

        PaymentMethod method = request.getPaymentMethod();
        if (method == PaymentMethod.PAY_LATER && !razorpayProperties.isPayLaterEnabled()) {
            throw new BadRequestException(
                    "PAY_LATER_UNAVAILABLE",
                    "Pay Later is required by the business but is not enabled for this Razorpay merchant account yet");
        }

        CheckoutPreviewRequest previewRequest = new CheckoutPreviewRequest();
        previewRequest.setPaymentMethod(method);
        CheckoutPreviewResponse preview = checkoutService.preview(customerId, guestToken, previewRequest);

        if (!preview.isValid() || preview.isRequiresReview() || !preview.isReadyToPlace()
                || !StringUtils.hasText(preview.getPreviewHash())) {
            throw new BadRequestException(
                    "CHECKOUT_REVIEW_REQUIRED",
                    "Checkout preview is not ready to place. Refresh preview and try again.");
        }

        if (!preview.getPreviewHash().equals(request.getPreviewHash().trim())) {
            throw new BadRequestException(
                    "CHECKOUT_REVIEW_REQUIRED",
                    "Checkout preview is stale. Refresh preview and try again.");
        }

        Cart cart = cartService.findActiveCartEntity(customerId, guestToken)
                .orElseThrow(() -> new BadRequestException("CART_EMPTY", "Cart is empty"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("CART_EMPTY", "Cart is empty");
        }

        String contactMobile = MobileNumberUtils.normalize(request.getContactMobile());
        PlaceOrderRequest.ShippingAddressRequest ship = request.getShippingAddress();
        String shipMobile = MobileNumberUtils.normalize(ship.getMobile());

        Order order = new Order();
        order.setOrderNumber(orderService.allocateOrderNumber());
        order.setCartId(cart.getId());
        order.setPaymentMethod(method);
        order.setCurrency(preview.getCurrency() != null ? preview.getCurrency() : "INR");
        order.setItemsSubtotal(money(preview.getItemsSubtotal()));
        order.setShippingCharge(money(preview.getShippingCharge()));
        order.setCodCharge(money(preview.getCodCharge()));
        order.setTaxAmount(money(preview.getTaxAmount()));
        order.setDiscountAmount(money(preview.getDiscountAmount()));
        order.setGrandTotal(money(preview.getGrandTotal()));
        order.setContactName(request.getContactName().trim());
        order.setContactMobile(contactMobile);
        order.setContactEmail(trimToNull(request.getContactEmail()));
        order.setPreviewHash(preview.getPreviewHash());
        order.setIdempotencyKey(idempotencyKey);

        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
            order.setCustomer(customer);
        } else {
            order.setCustomer(null);
            order.setGuestMobile(contactMobile);
            order.setGuestEmail(trimToNull(request.getContactEmail()));
        }

        if (method == PaymentMethod.COD) {
            order.setStatus(OrderStatus.PLACED);
            order.setPaymentStatus(PaymentStatus.COD_PENDING);
        } else if (ONLINE_METHODS.contains(method)) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setPaymentStatus(PaymentStatus.PENDING);
        } else {
            throw new BadRequestException("UNSUPPORTED_PAYMENT_METHOD", "Unsupported payment method");
        }

        for (var line : preview.getLines()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            Product product = productRepository.findById(line.getProductId()).orElse(null);
            item.setProduct(product);
            item.setProductTitleSnapshot(line.getTitle());
            item.setProductSlugSnapshot(line.getSlug());
            item.setUnitSellingPrice(money(line.getUnitSellingPrice()));
            item.setUnitMrpSnapshot(line.getUnitMrp() == null ? null : money(line.getUnitMrp()));
            item.setQuantity(line.getQuantity());
            item.setLineSubtotal(money(line.getLineSubtotal()));
            order.getItems().add(item);
        }

        OrderAddress address = new OrderAddress();
        address.setOrder(order);
        address.setFullName(ship.getFullName().trim());
        address.setMobile(shipMobile);
        address.setEmail(trimToNull(ship.getEmail() != null ? ship.getEmail() : request.getContactEmail()));
        address.setLine1(ship.getLine1().trim());
        address.setLine2(trimToNull(ship.getLine2()));
        address.setLandmark(trimToNull(ship.getLandmark()));
        address.setCity(ship.getCity().trim());
        address.setState(ship.getState().trim());
        address.setPostalCode(ship.getPostalCode().trim());
        address.setCountry(StringUtils.hasText(ship.getCountry()) ? ship.getCountry().trim() : "India");
        order.setShippingAddress(address);

        orderRepository.save(order);

        InitiatePaymentResponse razorpay = null;
        if (method == PaymentMethod.COD) {
            cartService.clearCartById(cart.getId());
            log.info("COD order placed orderNumber={} cartId={}", order.getOrderNumber(), cart.getId());
        } else {
            InitiatePaymentRequest payRequest = new InitiatePaymentRequest();
            payRequest.setOrderNumber(order.getOrderNumber());
            payRequest.setIdempotencyKey(paymentIdempotencyKey(idempotencyKey));
            razorpay = paymentService.initiate(payRequest);
            log.info("Prepaid order created orderNumber={} awaiting payment", order.getOrderNumber());
        }

        return toPlaceResponse(order, razorpay);
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderSummary(String orderNumber) {
        rejectAdminActingAsCustomer();
        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        assertOrderAccess(order);
        return toSummary(order);
    }

    private InitiatePaymentResponse maybeReinitiatePayment(Order order, String orderIdempotencyKey) {
        if (order.getPaymentMethod() == null || order.getPaymentMethod() == PaymentMethod.COD) {
            return null;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return null;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            return null;
        }
        InitiatePaymentRequest payRequest = new InitiatePaymentRequest();
        payRequest.setOrderNumber(order.getOrderNumber());
        payRequest.setIdempotencyKey(paymentIdempotencyKey(orderIdempotencyKey));
        return paymentService.initiate(payRequest);
    }

    private void assertIdempotentOrderAccess(Order order, Long customerId) {
        if (customerId != null) {
            if (order.getCustomer() == null || !customerId.equals(order.getCustomer().getId())) {
                throw new ForbiddenException("ORDER_ACCESS_DENIED", "Not allowed to reuse this idempotency key");
            }
            return;
        }
        if (order.getCustomer() != null) {
            throw new ForbiddenException("CUSTOMER_AUTH_REQUIRED", "Customer authentication is required");
        }
    }

    private void assertOrderAccess(Order order) {
        Optional<Long> customerId = SecurityUtils.currentCustomerId();
        if (customerId.isPresent()) {
            if (order.getCustomer() == null || !customerId.get().equals(order.getCustomer().getId())) {
                throw new ForbiddenException("ORDER_ACCESS_DENIED", "Not allowed to view this order");
            }
            return;
        }
        if (order.getCustomer() != null) {
            throw new ForbiddenException("CUSTOMER_AUTH_REQUIRED", "Customer authentication is required");
        }
    }

    private void rejectAdminActingAsCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails) {
            throw new ForbiddenException("ADMIN_NOT_CUSTOMER", "Admin credentials cannot place customer orders");
        }
    }

    private PlaceOrderResponse toPlaceResponse(Order order, InitiatePaymentResponse razorpay) {
        PlaceOrderResponse response = new PlaceOrderResponse();
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus().name());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        response.setCurrency(order.getCurrency());
        response.setItemsSubtotal(order.getItemsSubtotal());
        response.setShippingCharge(order.getShippingCharge());
        response.setCodCharge(order.getCodCharge());
        response.setTaxAmount(order.getTaxAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setGrandTotal(order.getGrandTotal());
        response.setRequiresOnlinePayment(order.getPaymentMethod() != PaymentMethod.COD
                && order.getPaymentStatus() != PaymentStatus.PAID
                && order.getPaymentStatus() != PaymentStatus.COD_PENDING);
        response.setRazorpay(razorpay);
        response.setItems(mapLines(order));
        return response;
    }

    private OrderSummaryResponse toSummary(Order order) {
        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus().name());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        response.setCurrency(order.getCurrency());
        response.setGrandTotal(order.getGrandTotal());
        response.setContactName(order.getContactName());
        response.setContactMobile(order.getContactMobile());
        if (order.getShippingAddress() != null) {
            response.setShippingCity(order.getShippingAddress().getCity());
            response.setShippingState(order.getShippingAddress().getState());
            response.setShippingPostalCode(order.getShippingAddress().getPostalCode());
        }
        response.setItems(mapLines(order));
        return response;
    }

    private List<PlaceOrderResponse.OrderLineSummary> mapLines(Order order) {
        List<PlaceOrderResponse.OrderLineSummary> lines = new ArrayList<>();
        if (order.getItems() == null) {
            return lines;
        }
        for (OrderItem item : order.getItems()) {
            PlaceOrderResponse.OrderLineSummary line = new PlaceOrderResponse.OrderLineSummary();
            line.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
            line.setTitle(item.getProductTitleSnapshot());
            line.setQuantity(item.getQuantity());
            line.setUnitSellingPrice(item.getUnitSellingPrice());
            line.setLineSubtotal(item.getLineSubtotal());
            lines.add(line);
        }
        return lines;
    }

    private static String paymentIdempotencyKey(String orderIdempotencyKey) {
        return orderIdempotencyKey + ":pay";
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUND);
        }
        return value.setScale(MONEY_SCALE, MONEY_ROUND);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
