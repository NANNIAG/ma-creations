package com.macreations.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.macreations.dto.InitiatePaymentRequest;
import com.macreations.dto.InitiatePaymentResponse;
import com.macreations.dto.VerifyPaymentRequest;
import com.macreations.dto.VerifyPaymentResponse;
import com.macreations.entity.Order;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentProvider;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.PaymentTransaction;
import com.macreations.entity.PaymentTransactionStatus;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.ForbiddenException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.OrderRepository;
import com.macreations.repository.PaymentTransactionRepository;
import com.macreations.security.AdminUserDetails;
import com.macreations.security.SecurityUtils;
import com.macreations.service.CartService;
import com.macreations.service.payment.PaymentGateway.CreatePaymentCommand;
import com.macreations.service.payment.PaymentGateway.CreatePaymentResult;
import com.macreations.service.payment.PaymentGateway.ParsedWebhookEvent;
import com.macreations.service.payment.PaymentGateway.VerifyPaymentCommand;
import com.macreations.service.payment.razorpay.RazorpayPaymentGateway;
import com.macreations.service.payment.razorpay.RazorpayProperties;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Set<PaymentMethod> ONLINE_METHODS = Set.of(
            PaymentMethod.UPI,
            PaymentMethod.CARD,
            PaymentMethod.NET_BANKING,
            PaymentMethod.PAY_LATER);

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final RazorpayProperties razorpayProperties;
    private final CartService cartService;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentGateway paymentGateway,
            RazorpayProperties razorpayProperties,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentGateway = paymentGateway;
        this.razorpayProperties = razorpayProperties;
        this.cartService = cartService;
    }

    @Transactional
    public InitiatePaymentResponse initiate(InitiatePaymentRequest request) {
        rejectAdminActingAsCustomer();
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber().trim())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        assertOrderAccess(order);
        assertOrderPayable(order);

        PaymentMethod method = order.getPaymentMethod();
        if (method == PaymentMethod.COD) {
            throw new BadRequestException(
                    "COD_NOT_ONLINE",
                    "COD does not use online payment gateway processing");
        }
        if (method == null || !ONLINE_METHODS.contains(method)) {
            throw new BadRequestException("UNSUPPORTED_PAYMENT_METHOD", "Unsupported payment method for online payment");
        }
        if (method == PaymentMethod.PAY_LATER && !razorpayProperties.isPayLaterEnabled()) {
            throw new BadRequestException(
                    "PAY_LATER_UNAVAILABLE",
                    "Pay Later is required by the business but is not enabled for this Razorpay merchant account yet");
        }

        String idempotencyKey = request.getIdempotencyKey().trim();
        Optional<PaymentTransaction> existing = paymentTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PaymentTransaction tx = existing.get();
            if (!tx.getOrder().getId().equals(order.getId())) {
                throw new BadRequestException("IDEMPOTENCY_CONFLICT", "Idempotency key already used for another order");
            }
            if (tx.getStatus() == PaymentTransactionStatus.CAPTURED) {
                throw new BadRequestException("PAYMENT_ALREADY_CAPTURED", "Payment already completed for this key");
            }
            if (StringUtils.hasText(tx.getProviderOrderId())
                    && (tx.getStatus() == PaymentTransactionStatus.PENDING
                    || tx.getStatus() == PaymentTransactionStatus.CREATED
                    || tx.getStatus() == PaymentTransactionStatus.AUTHORIZED)) {
                return toInitiateResponse(order, tx);
            }
        }

        BigDecimal amount = money(order.getGrandTotal());
        PaymentTransaction tx = existing.orElseGet(PaymentTransaction::new);
        tx.setOrder(order);
        tx.setProvider(PaymentProvider.RAZORPAY);
        tx.setPaymentMethod(method);
        tx.setAmount(amount);
        tx.setCurrency(order.getCurrency() != null ? order.getCurrency() : "INR");
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(PaymentTransactionStatus.CREATED);
        tx.setFailureCode(null);
        paymentTransactionRepository.save(tx);

        Map<String, String> notes = new HashMap<>();
        notes.put("order_number", order.getOrderNumber());
        notes.put("payment_method", method.name());
        notes.put("payment_tx_id", String.valueOf(tx.getId()));

        CreatePaymentResult created;
        try {
            created = paymentGateway.createPayment(new CreatePaymentCommand(
                    order.getOrderNumber(),
                    amount,
                    tx.getCurrency(),
                    method,
                    notes));
        } catch (PaymentGatewayException ex) {
            tx.setStatus(PaymentTransactionStatus.FAILED);
            tx.setFailureCode(ex.getCode());
            paymentTransactionRepository.save(tx);
            throw new BadRequestException(ex.getCode(), ex.getMessage());
        }

        tx.setProviderOrderId(created.providerOrderId());
        tx.setProviderReference(created.providerReference());
        tx.setStatus(PaymentTransactionStatus.PENDING);
        paymentTransactionRepository.save(tx);

        log.info("Payment initiated orderNumber={} paymentTxId={} providerOrderId={}",
                order.getOrderNumber(), tx.getId(), created.providerOrderId());

        return toInitiateResponse(order, tx);
    }

    @Transactional
    public VerifyPaymentResponse verify(VerifyPaymentRequest request) {
        rejectAdminActingAsCustomer();
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber().trim())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        assertOrderAccess(order);

        PaymentTransaction tx = paymentTransactionRepository
                .findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, request.getRazorpayOrderId().trim())
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment transaction not found"));

        if (!tx.getOrder().getId().equals(order.getId())) {
            throw new ForbiddenException("PAYMENT_ORDER_MISMATCH", "Payment does not belong to this order");
        }

        if (tx.getStatus() == PaymentTransactionStatus.CAPTURED
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            return toVerifyResponse(order, true);
        }

        boolean valid;
        try {
            valid = paymentGateway.verifyCheckoutSignature(new VerifyPaymentCommand(
                    request.getRazorpayOrderId().trim(),
                    request.getRazorpayPaymentId().trim(),
                    request.getRazorpaySignature().trim()));
        } catch (PaymentGatewayException ex) {
            throw new BadRequestException(ex.getCode(), ex.getMessage());
        }

        if (!valid) {
            throw new BadRequestException("INVALID_PAYMENT_SIGNATURE", "Payment signature verification failed");
        }

        markCaptured(tx, order, request.getRazorpayPaymentId().trim(), null);
        return toVerifyResponse(order, true);
    }

    @Transactional
    public void handleRazorpayWebhook(String rawBody, String signatureHeader) {
        boolean valid;
        try {
            valid = paymentGateway.verifyWebhookSignature(rawBody, signatureHeader);
        } catch (PaymentGatewayException ex) {
            throw new ForbiddenException(ex.getCode(), "Webhook signature verification failed");
        }
        if (!valid) {
            throw new ForbiddenException("INVALID_WEBHOOK_SIGNATURE", "Invalid Razorpay webhook signature");
        }

        ParsedWebhookEvent event = paymentGateway.parseWebhookEvent(rawBody);
        if (StringUtils.hasText(event.eventId())) {
            Optional<PaymentTransaction> byEvent = paymentTransactionRepository.findByLastWebhookEventId(event.eventId());
            if (byEvent.isPresent()) {
                log.info("Ignoring duplicate Razorpay webhook eventId={}", event.eventId());
                return;
            }
        }

        String eventType = event.eventType() == null ? "" : event.eventType();
        log.info("Razorpay webhook eventType={} eventId={} providerOrderId={}",
                eventType, event.eventId(), event.providerOrderId());

        PaymentTransaction tx = resolveTransaction(event);
        if (tx == null) {
            log.warn("No payment transaction for Razorpay webhook eventType={}", eventType);
            return;
        }

        Order order = tx.getOrder();

        if (tx.getStatus() == PaymentTransactionStatus.CAPTURED
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            if (StringUtils.hasText(event.eventId())) {
                tx.setLastWebhookEventId(event.eventId());
                paymentTransactionRepository.save(tx);
            }
            return;
        }

        if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
            markCaptured(tx, order, event.providerPaymentId(), event.eventId());
            return;
        }

        if ("payment.failed".equals(eventType)) {
            markFailed(tx, order, event.eventId(), "PAYMENT_FAILED");
            return;
        }

        if ("payment.authorized".equals(eventType)) {
            if (tx.getStatus() != PaymentTransactionStatus.CAPTURED) {
                tx.setStatus(PaymentTransactionStatus.AUTHORIZED);
                if (StringUtils.hasText(event.providerPaymentId())) {
                    tx.setProviderPaymentId(event.providerPaymentId());
                }
                if (StringUtils.hasText(event.eventId())) {
                    tx.setLastWebhookEventId(event.eventId());
                }
                paymentTransactionRepository.save(tx);
            }
        }
    }

    private PaymentTransaction resolveTransaction(ParsedWebhookEvent event) {
        if (StringUtils.hasText(event.providerPaymentId())) {
            Optional<PaymentTransaction> byPayment = paymentTransactionRepository
                    .findByProviderAndProviderPaymentId(PaymentProvider.RAZORPAY, event.providerPaymentId());
            if (byPayment.isPresent()) {
                return byPayment.get();
            }
        }
        if (StringUtils.hasText(event.providerOrderId())) {
            return paymentTransactionRepository
                    .findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, event.providerOrderId())
                    .orElse(null);
        }
        return null;
    }

    private void markCaptured(PaymentTransaction tx, Order order, String providerPaymentId, String eventId) {
        if (StringUtils.hasText(providerPaymentId)) {
            tx.setProviderPaymentId(providerPaymentId);
        }
        tx.setStatus(PaymentTransactionStatus.CAPTURED);
        tx.setFailureCode(null);
        if (StringUtils.hasText(eventId)) {
            tx.setLastWebhookEventId(eventId);
        }
        paymentTransactionRepository.save(tx);

        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            order.setStatus(OrderStatus.PLACED);
            if (order.getConfirmedAt() == null) {
                order.setConfirmedAt(Instant.now());
            }
        }
        orderRepository.save(order);
        cartService.clearCartById(order.getCartId());
    }

    /**
     * Marks the latest open payment attempt failed after a checkout cancel/error.
     * Does not delete the order; cart is retained for recovery.
     */
    @Transactional
    public VerifyPaymentResponse reportFailure(String orderNumber) {
        rejectAdminActingAsCustomer();
        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        assertOrderAccess(order);
        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentMethod() == PaymentMethod.COD) {
            return toVerifyResponse(order, order.getPaymentStatus() == PaymentStatus.PAID);
        }

        paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .findFirst()
                .ifPresent(tx -> {
                    if (tx.getStatus() != PaymentTransactionStatus.CAPTURED) {
                        markFailed(tx, order, null, "CHECKOUT_FAILED");
                    }
                });
        return toVerifyResponse(order, false);
    }

    private void markFailed(PaymentTransaction tx, Order order, String eventId, String failureCode) {
        if (tx.getStatus() == PaymentTransactionStatus.CAPTURED) {
            return;
        }
        tx.setStatus(PaymentTransactionStatus.FAILED);
        tx.setFailureCode(failureCode);
        if (StringUtils.hasText(eventId)) {
            tx.setLastWebhookEventId(eventId);
        }
        paymentTransactionRepository.save(tx);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }
    }

    private void assertOrderPayable(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("ORDER_ALREADY_PAID", "Order is already paid");
        }
        if (order.getPaymentStatus() == PaymentStatus.COD_PENDING
                || order.getPaymentMethod() == PaymentMethod.COD) {
            throw new BadRequestException("COD_NOT_ONLINE", "COD orders are not paid online");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new BadRequestException("ORDER_NOT_PAYABLE", "Order is not in a payable state");
        }
        if (order.getGrandTotal() == null || order.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("INVALID_ORDER_AMOUNT", "Order amount is invalid");
        }
    }

    private void assertOrderAccess(Order order) {
        Optional<Long> customerId = SecurityUtils.currentCustomerId();
        if (customerId.isPresent()) {
            if (order.getCustomer() == null || !customerId.get().equals(order.getCustomer().getId())) {
                throw new ForbiddenException("ORDER_ACCESS_DENIED", "Not allowed to pay for this order");
            }
            return;
        }
        if (order.getCustomer() != null) {
            throw new ForbiddenException("CUSTOMER_AUTH_REQUIRED", "Customer authentication is required for this order");
        }
        // Guest order: possession of order number is temporary access until guest tracking is designed.
    }

    private void rejectAdminActingAsCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails) {
            throw new ForbiddenException("ADMIN_NOT_CUSTOMER", "Admin credentials cannot initiate customer payments");
        }
    }

    private InitiatePaymentResponse toInitiateResponse(Order order, PaymentTransaction tx) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setPaymentTransactionId(tx.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setKeyId(razorpayProperties.getKeyId());
        response.setRazorpayOrderId(tx.getProviderOrderId());
        response.setAmount(RazorpayPaymentGateway.toPaise(tx.getAmount()));
        response.setCurrency(tx.getCurrency());
        response.setAmountInr(tx.getAmount());
        response.setPaymentMethod(tx.getPaymentMethod().name());
        return response;
    }

    private VerifyPaymentResponse toVerifyResponse(Order order, boolean paid) {
        VerifyPaymentResponse response = new VerifyPaymentResponse();
        response.setOrderNumber(order.getOrderNumber());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setOrderStatus(order.getStatus().name());
        response.setPaid(paid);
        return response;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
